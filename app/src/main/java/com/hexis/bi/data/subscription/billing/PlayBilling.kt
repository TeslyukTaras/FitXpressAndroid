package com.hexis.bi.data.subscription.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.hexis.bi.data.subscription.PLAN_PRODUCT_IDS
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import kotlin.coroutines.resume

class PlayBillingException(message: String) : Exception(message)

data class PlanOffer(
    val productId: String,
    val offerToken: String,
    val formattedPrice: String,
)

sealed interface PurchaseOutcome {
    data class Purchased(val purchase: Purchase) : PurchaseOutcome
    data object Pending : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data object Deferred : PurchaseOutcome
}

enum class ReplacementKind { UPGRADE, DOWNGRADE }

class PlayBilling(
    context: Context,
    private val activityProvider: CurrentActivityProvider,
) {
    private val connectMutex = Mutex()
    private val purchaseMutex = Mutex()
    @Volatile
    private var launchResult: CompletableDeferred<PurchaseOutcome>? = null
    @Volatile
    private var cachedDetails: Map<String, ProductDetails> = emptyMap()

    private val _unsolicited = MutableSharedFlow<Purchase>(extraBufferCapacity = SubscriptionConstants.BILLING_UNSOLICITED_BUFFER)
    val unsolicitedPurchases: SharedFlow<Purchase> = _unsolicited.asSharedFlow()
    val foreground: SharedFlow<Unit> get() = activityProvider.resumed

    private val listener = PurchasesUpdatedListener { result, purchases ->
        val purchased = purchases.orEmpty().firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val outcome: PurchaseOutcome? = when {
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.Cancelled
            result.responseCode != BillingClient.BillingResponseCode.OK -> null
            purchased != null -> PurchaseOutcome.Purchased(purchased)
            purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseOutcome.Pending
            purchases.isNullOrEmpty() && launchResult != null -> PurchaseOutcome.Deferred
            else -> null
        }
        val waiting = launchResult
        when {
            waiting != null && outcome != null -> waiting.complete(outcome)
            waiting != null -> waiting.completeExceptionally(PlayBillingException(result.debugMessage.ifBlank { "Billing error ${result.responseCode}" }))
            purchased != null -> _unsolicited.tryEmit(purchased)
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    suspend fun offers(): Map<String, PlanOffer> =
        productDetails().mapNotNull { (productId, details) ->
            val offer = details.baseOffer() ?: return@mapNotNull null
            productId to PlanOffer(
                productId = productId,
                offerToken = offer.offerToken,
                formattedPrice = offer.pricingPhases.pricingPhaseList.first().formattedPrice,
            )
        }.toMap()

    suspend fun ownedSubscriptions(): List<Purchase> {
        ensureConnected()
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        )
        requireOk(result.billingResult)
        return result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
    }

    suspend fun purchase(
        productId: String,
        accountId: String,
        replace: Pair<Purchase, ReplacementKind>? = null,
    ): PurchaseOutcome = purchaseMutex.withLock {
        val activity = activityProvider.get()
            ?: throw PlayBillingException("No foreground activity to start the purchase")
        val details = productDetails(refresh = true)[productId]
            ?: throw PlayBillingException("Product $productId is not available")
        val offer = details.baseOffer()
            ?: throw PlayBillingException("Product $productId has no base plan to buy")

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
            .build()
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(accountId)
        if (replace != null) {
            flow.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(replace.first.purchaseToken)
                    .setSubscriptionReplacementMode(replace.second.toReplacementMode())
                    .build(),
            )
        }

        val waiting = CompletableDeferred<PurchaseOutcome>()
        launchResult = waiting
        try {
            val launched = client.launchBillingFlow(activity, flow.build())
            if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
                if (launched.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                    return@withLock PurchaseOutcome.Cancelled
                }
                requireOk(launched)
            }
            waiting.await()
        } catch (e: PlayBillingException) {
            cachedDetails = emptyMap()
            throw e
        } finally {
            launchResult = null
        }
    }

    private suspend fun productDetails(refresh: Boolean = false): Map<String, ProductDetails> {
        if (!refresh) cachedDetails.takeIf { it.keys.containsAll(PLAN_PRODUCT_IDS) }?.let { return it }
        ensureConnected()
        val result = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    PLAN_PRODUCT_IDS.map { productId ->
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    },
                )
                .build(),
        )
        requireOk(result.billingResult)
        val details = result.productDetailsList.orEmpty().associateBy { it.productId }
        if (details.isEmpty()) throw PlayBillingException("No subscription products are available")
        cachedDetails = details
        return details
    }

    private fun ProductDetails.baseOffer(): ProductDetails.SubscriptionOfferDetails? =
        subscriptionOfferDetails.orEmpty().firstOrNull {
            it.offerId == null && it.basePlanId == SubscriptionConstants.PLAY_BASE_PLAN_ID
        }

    private fun ReplacementKind.toReplacementMode(): Int = when (this) {
        ReplacementKind.UPGRADE -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
        ReplacementKind.DOWNGRADE -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
    }

    private suspend fun ensureConnected() {
        if (client.isReady) return
        connectMutex.withLock {
            if (client.isReady) return
            val result = suspendCancellableCoroutine { continuation ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (continuation.isActive) continuation.resume(billingResult)
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.w("Play Billing service disconnected")
                    }
                })
            }
            requireOk(result)
        }
    }

    private fun requireOk(result: BillingResult) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw PlayBillingException(
                "Play Billing error ${result.responseCode}: ${result.debugMessage}",
            )
        }
    }
}
