package com.hexis.bi.data.telemetry

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.appsflyer.AppsFlyerLib
import com.appsflyer.api.PurchaseClient
import com.appsflyer.api.Store
import com.appsflyer.share.SessionReadyListener
import com.appsflyer.share.attribution.AppsFlyerRequestListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.hexis.bi.BuildConfig
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.utils.constants.MarketingConfig
import com.hexis.bi.utils.constants.MarketingEvents
import com.hexis.bi.utils.constants.MarketingMethods
import com.hexis.bi.utils.constants.MarketingParams
import timber.log.Timber

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
        if (ready) {
            event()
        } else if (loading) {
            if (pendingEvents.size >= MarketingConfig.MAX_PENDING_EVENTS) pendingEvents.removeAt(0)
            pendingEvents.add(event)
        }
    }

    @Synchronized private fun finishLoading(enabled: Boolean) {
        ready = enabled
        loading = false
        if (enabled) pendingEvents.forEach { it() }
        pendingEvents.clear()
    }

    private var started = false
    private var lastAttemptAt = 0L
    private val retryHandler = Handler(Looper.getMainLooper())

    @Synchronized private fun scheduleRetry(context: Context) {
        started = false
        retryHandler.removeCallbacksAndMessages(null)
        retryHandler.postDelayed({ start(context) }, MarketingConfig.START_RETRY_INTERVAL_MS)
    }

    @Synchronized fun start(context: Context) {
        if (started) return
        val now = SystemClock.elapsedRealtime()
        if (lastAttemptAt != 0L && now - lastAttemptAt < MarketingConfig.START_RETRY_INTERVAL_MS) return
        lastAttemptAt = now
        started = true
        retryHandler.removeCallbacksAndMessages(null)
        val app = context.applicationContext
        val config = FirebaseRemoteConfig.getInstance()
        config.setDefaultsAsync(mapOf(MarketingConfig.REMOTE_KEY_PURCHASE_CONNECTOR to false))
        config.fetchAndActivate().addOnCompleteListener {
            val key = config.getString(MarketingConfig.REMOTE_KEY_SDK_KEY)
            if (key.isBlank()) {
                Timber.w("AppsFlyer not started: no dev key in Remote Config, will retry")
                scheduleRetry(app)
                return@addOnCompleteListener
            }
            val appsFlyer = AppsFlyerLib.getInstance()
            appsFlyer.setDebugLog(BuildConfig.DEBUG)
            appsFlyer.init(key, null, app)
            appsFlyer.registerSessionReadyListener(object : SessionReadyListener {
                override fun onSessionReady() {
                    Timber.i("AppsFlyer session ready")
                }
            })
            appsFlyer.start(object : AppsFlyerRequestListener {
                override fun onSuccess() {
                    Timber.i("AppsFlyer started")
                }

                override fun onError(code: Int, description: String) {
                    Timber.w("AppsFlyer start failed code=%d %s", code, description)
                }
            })
            val connectorEnabled = config.getBoolean(MarketingConfig.REMOTE_KEY_PURCHASE_CONNECTOR)
            if (connectorEnabled) {
                runCatching {
                    purchases = PurchaseClient.Builder(app, Store.GOOGLE)
                        .logSubscriptions(true)
                        .setSandbox(config.getBoolean(MarketingConfig.REMOTE_KEY_SANDBOX))
                        .build()
                    purchases?.startObservingTransactions()
                }.onFailure { Timber.w(it, "Purchase connector failed to start") }
            }
            finishLoading(true)
        }
    }

    private fun pending(context: Context) =
        context.getSharedPreferences(MarketingConfig.PENDING_REGISTRATION_PREFS, Context.MODE_PRIVATE)

    fun newAccount(context: Context, uid: String, method: Method) {
        pending(context).edit().putString(uid, method.label).apply()
    }

    fun signedIn(context: Context, method: Method) {
        val app = context.applicationContext
        start(app)
        whenReady {
            AppsFlyerLib.getInstance().logEvent(
                app,
                MarketingEvents.LOGIN,
                mapOf(MarketingParams.LOGIN_METHOD to method.label),
            )
        }
    }

    @Synchronized fun profileSaved(context: Context) {
        val app = context.applicationContext
        start(app)
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val prefs = pending(context)
        val method = prefs.getString(user.uid, null) ?: return
        if (method == Method.EMAIL.label && !user.isEmailVerified) return
        whenReady {
            if (!prefs.contains(user.uid)) return@whenReady
            AppsFlyerLib.getInstance().logEvent(
                app,
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
    ) {
        val app = context.applicationContext
        start(app)
        whenReady {
            AppsFlyerLib.getInstance().logEvent(
                app,
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
}
