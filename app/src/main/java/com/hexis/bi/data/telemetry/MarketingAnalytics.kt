package com.hexis.bi.data.telemetry

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.api.PurchaseClient
import com.appsflyer.api.Store
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.utils.constants.MarketingConfig
import com.hexis.bi.utils.constants.MarketingEvents
import com.hexis.bi.utils.constants.MarketingMethods
import com.hexis.bi.utils.constants.MarketingParams

internal object MarketingAnalytics {
    enum class Method(val label: String) {
        EMAIL(MarketingMethods.EMAIL),
        GOOGLE(MarketingMethods.GOOGLE),
        APPLE(MarketingMethods.APPLE),
    }

    private var loading = true
    private var ready = false
    private val pendingEvents = mutableListOf<() -> Unit>()
    private var purchases: PurchaseClient? = null

    @Synchronized private fun whenReady(event: () -> Unit) {
        if (ready) event() else if (loading) pendingEvents.add(event)
    }

    @Synchronized private fun finishLoading(enabled: Boolean) {
        ready = enabled
        loading = false
        if (enabled) pendingEvents.forEach { it() }
        pendingEvents.clear()
    }

    fun start(context: Context) {
        val config = FirebaseRemoteConfig.getInstance()
        config.fetchAndActivate().addOnCompleteListener {
            val key = config.getString(MarketingConfig.REMOTE_KEY_SDK_KEY)
            if (key.isBlank()) {
                finishLoading(false)
                return@addOnCompleteListener
            }
            AppsFlyerLib.getInstance().apply {
                init(key, null, context)
                start(context)
            }
            purchases = PurchaseClient.Builder(context, Store.GOOGLE)
                .logSubscriptions(true)
                .setSandbox(config.getBoolean(MarketingConfig.REMOTE_KEY_SANDBOX))
                .build()
            purchases?.startObservingTransactions()
            finishLoading(true)
        }
    }

    private fun pending(context: Context) =
        context.getSharedPreferences(MarketingConfig.PENDING_REGISTRATION_PREFS, Context.MODE_PRIVATE)

    fun newAccount(context: Context, uid: String, method: Method) {
        pending(context).edit().putString(uid, method.label).apply()
    }

    fun signedIn(context: Context, method: Method) {
        whenReady {
            AppsFlyerLib.getInstance().logEvent(
                context,
                MarketingEvents.LOGIN,
                mapOf(MarketingParams.LOGIN_METHOD to method.label),
            )
        }
    }

    @Synchronized fun profileSaved(context: Context) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = pending(context)
        val method = prefs.getString(user.uid, null) ?: return
        if (method == Method.EMAIL.label && !user.isEmailVerified) return
        whenReady {
            if (!prefs.contains(user.uid)) return@whenReady
            AppsFlyerLib.getInstance().logEvent(
                context,
                MarketingEvents.COMPLETE_REGISTRATION,
                mapOf(MarketingParams.REGISTRATION_METHOD to method),
            )
            prefs.edit().remove(user.uid).apply()
        }
    }

    fun planSelected(context: Context, plan: PlanId, productId: String, priceMicros: Long, currency: String) =
        commerce(context, MarketingEvents.PLAN_SELECTED, plan, productId, priceMicros, currency)

    fun checkoutStarted(context: Context, plan: PlanId, productId: String, priceMicros: Long, currency: String) =
        commerce(context, MarketingEvents.INITIATED_CHECKOUT, plan, productId, priceMicros, currency)

    private fun commerce(
        context: Context,
        event: String,
        plan: PlanId,
        productId: String,
        priceMicros: Long,
        currency: String,
    ) = whenReady {
        AppsFlyerLib.getInstance().logEvent(
            context,
            event,
            mapOf(
                MarketingParams.PLAN_ID to plan.name.lowercase(),
                MarketingParams.PRODUCT_ID to productId,
                MarketingParams.DISPLAYED_PRICE to priceMicros / MarketingConfig.MICROS_PER_UNIT,
                MarketingParams.CURRENCY to currency,
            ),
        )
    }
}
