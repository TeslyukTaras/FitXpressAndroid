package com.hexis.bi.data.subscription

import android.content.Context
import java.util.concurrent.atomic.AtomicBoolean
import com.android.billingclient.api.Purchase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctions
import com.hexis.bi.R
import com.hexis.bi.data.subscription.billing.PlayBilling
import com.hexis.bi.data.subscription.billing.PlayBillingException
import com.hexis.bi.data.subscription.billing.PurchaseOutcome
import com.hexis.bi.data.subscription.billing.ReplacementKind
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.PurchaseResult
import com.hexis.bi.domain.subscription.RestoreResult
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.utils.constants.SubscriptionConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

class FirestoreSubscriptionRepository(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val billing: PlayBilling,
    private val catalog: PlanCatalog,
    scope: CoroutineScope,
) : SubscriptionRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val status: StateFlow<SubscriptionStatus> = signedInUid()
        .flatMapLatest { uid ->
            if (uid == null) flowOf<SubscriptionStatus>(SubscriptionStatus.None())
            else flow {
                emit(SubscriptionStatus.Loading)
                emitAll(entitlement(uid))
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, SubscriptionStatus.Loading)

    init {
        scope.launch {
            billing.unsolicitedPurchases.collect { purchase ->
                if (purchase.belongsTo(auth.currentUser?.uid)) {
                    runCatching { verify(purchase) }
                        .onFailure { Timber.w(it, "Verifying an unsolicited purchase failed") }
                }
            }
        }
        scope.launch {
            billing.foreground.collect {
                val uid = auth.currentUser?.uid ?: return@collect
                runCatching { reconcileUnverified(uid) }
                    .onFailure { Timber.w(it, "Reconciling owned purchases on resume failed") }
            }
        }
        scope.launch {
            signedInUid().collectLatest { uid ->
                if (uid != null) runCatching { reconcileUnverified(uid) }
                    .onFailure { Timber.w(it, "Reconciling owned purchases failed") }
            }
        }
    }

    override suspend fun loadPlans(): List<SubscriptionPlan> {
        catalog.refresh()
        val offers = billingCall { billing.offers() }
        val info = catalog.plans.value
        val plans = PlanId.entries.mapNotNull { id ->
            val offer = offers[id.productId()] ?: return@mapNotNull null
            val details = info.getValue(id)
            SubscriptionPlan(
                id = id,
                formattedPrice = offer.formattedPrice,
                scansPerMonth = details.scansPerMonth,
                rank = details.rank,
                mostPopular = details.mostPopular,
            )
        }
        if (plans.isEmpty()) throw IllegalStateException(context.getString(R.string.subscription_error_unavailable))
        return plans
    }

    override suspend fun subscribe(plan: PlanId): PurchaseResult {
        val current = settledStatus()
            ?: throw IllegalStateException(context.getString(R.string.subscription_error_unavailable))
        if (current is SubscriptionStatus.Expired && current.paymentIssue) {
            throw IllegalStateException(context.getString(R.string.subscription_error_payment_issue))
        }
        return purchase(plan, replace = null)
    }

    override suspend fun changePlan(plan: PlanId): PurchaseResult {
        val settled = settledStatus()
            ?: throw IllegalStateException(context.getString(R.string.subscription_error_unavailable))
        val current = settled as? SubscriptionStatus.Subscribed
            ?: throw IllegalStateException(context.getString(R.string.subscription_error_no_purchase))
        if (current.platform != StorePlatform.ANDROID) {
            throw IllegalStateException(context.getString(R.string.subscription_error_other_platform))
        }
        val uid = auth.currentUser?.uid
        val owned = billingCall { billing.ownedSubscriptions() }
            .firstOrNull { it.isPlanProduct() && it.belongsTo(uid) }
            ?: throw IllegalStateException(context.getString(R.string.subscription_error_no_purchase))
        if (plan == current.plan) return PurchaseResult.COMPLETED
        val ranks = catalog.plans.value
        val kind = if (ranks.getValue(plan).rank < ranks.getValue(current.plan).rank) {
            ReplacementKind.UPGRADE
        } else {
            ReplacementKind.DOWNGRADE
        }
        return purchase(plan, replace = owned to kind)
    }

    override suspend fun restorePurchases(): RestoreResult {
        val current = settledStatus() ?: return RestoreResult.FAILED
        val subscribed = current as? SubscriptionStatus.Subscribed
        if (subscribed != null && subscribed.platform != StorePlatform.ANDROID) return RestoreResult.RESTORED
        val uid = auth.currentUser?.uid
        val owned = billingCall { billing.ownedSubscriptions() }
            .filter { it.isPlanProduct() && it.belongsTo(uid) }
        if (owned.isEmpty()) return if (subscribed != null) RestoreResult.RESTORED else RestoreResult.NOT_FOUND
        val restored = owned.map { verify(it) }.any { it in ACCESS_STATES }
        return if (restored) RestoreResult.RESTORED else RestoreResult.NOT_FOUND
    }

    private suspend fun purchase(plan: PlanId, replace: Pair<Purchase, ReplacementKind>?): PurchaseResult {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException(context.getString(R.string.subscription_error_signed_out))
        return when (val outcome = billingCall { billing.purchase(plan.productId(), uid, replace) }) {
            is PurchaseOutcome.Purchased -> {
                verify(outcome.purchase)
                PurchaseResult.COMPLETED
            }

            PurchaseOutcome.Pending ->
                throw IllegalStateException(context.getString(R.string.subscription_error_pending))

            PurchaseOutcome.Deferred -> {
                val owned = replace?.first
                    ?: throw IllegalStateException(context.getString(R.string.subscription_error_unavailable))
                verify(owned)
                PurchaseResult.COMPLETED
            }

            PurchaseOutcome.Cancelled -> PurchaseResult.CANCELLED
        }
    }

    private suspend fun reconcileUnverified(uid: String) {
        billing.ownedSubscriptions()
            .filter { it.isPlanProduct() && it.belongsTo(uid) && !it.isAcknowledged }
            .forEach { verify(it) }
    }

    private suspend fun settledStatus(): SubscriptionStatus? =
        withTimeoutOrNull(SubscriptionConstants.STATUS_WAIT_MS) {
            status.first { it !is SubscriptionStatus.Loading }
        }

    private suspend fun <T> billingCall(block: suspend () -> T): T =
        try {
            block()
        } catch (e: PlayBillingException) {
            Timber.w(e, "Play Billing call failed")
            throw IllegalStateException(context.getString(R.string.subscription_error_unavailable), e)
        }

    private fun Purchase.isPlanProduct(): Boolean = products.any { it in PLAN_PRODUCT_IDS }

    private fun Purchase.belongsTo(uid: String?): Boolean =
        uid != null && accountIdentifiers?.obfuscatedAccountId == uid

    private suspend fun verify(purchase: Purchase): String? {
        val payload = mapOf(
            SubscriptionConstants.FIELD_PLATFORM to SubscriptionConstants.PLATFORM_ANDROID,
            SubscriptionConstants.FIELD_PRODUCT_ID to purchase.products.first { it in PLAN_PRODUCT_IDS },
            SubscriptionConstants.FIELD_PURCHASE_TOKEN to purchase.purchaseToken,
        )
        val result = functions.getHttpsCallable(SubscriptionConstants.FUNCTION_VERIFY_PURCHASE)
            .call(payload)
            .await()
        return (result.data as? Map<*, *>)?.get(SubscriptionConstants.FIELD_STATE) as? String
    }

    private fun signedInUid(): Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    private fun entitlement(uid: String): Flow<SubscriptionStatus> {
        val freeScanUsed = AtomicBoolean(false)
        return entitlementUpdates(uid, freeScanUsed)
    }

    private fun entitlementUpdates(uid: String, freeScanUsed: AtomicBoolean): Flow<SubscriptionStatus> = callbackFlow {
        val registration = firestore.collection(SubscriptionConstants.COLLECTION_SUBSCRIPTIONS)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        trySend(SubscriptionStatus.None(freeScanUsed.get()))
                    }
                    close(error)
                } else if (snapshot != null) {
                    val status = snapshot.toSubscriptionStatus()
                    freeScanUsed.set(status !is SubscriptionStatus.None || status.freeScanUsed)
                    trySend(status)
                }
            }
        awaitClose { registration.remove() }
    }.retryWhen { cause, attempt ->
        Timber.w(cause, "Subscription listener failed, retrying")
        delay(
            minOf(
                SubscriptionConstants.SNAPSHOT_RETRY_BASE_MS shl attempt.coerceAtMost(SubscriptionConstants.SNAPSHOT_RETRY_MAX_SHIFT).toInt(),
                SubscriptionConstants.SNAPSHOT_RETRY_MAX_MS,
            ),
        )
        true
    }
}
