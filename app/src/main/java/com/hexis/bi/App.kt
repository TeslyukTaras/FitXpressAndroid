package com.hexis.bi

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.hexis.bi.R
import com.hexis.bi.data.health.access.HealthDataGate
import com.hexis.bi.data.terra.TerraSdkSync
import com.hexis.bi.data.subscription.billing.CurrentActivityProvider
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.reminder.ScanReminderWorkRunner
import com.hexis.bi.data.store.PendingFirestoreCacheWipe
import com.hexis.bi.data.telemetry.Telemetry
import com.hexis.bi.di.appModule
import com.hexis.bi.utils.CrashlyticsTree
import com.hexis.bi.utils.SystemNotificationHelper
import com.hexis.bi.utils.constants.AnimationConstants
import com.look.camera.sdk.SdkActivity
import java.util.Collections
import java.util.WeakHashMap
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import timber.log.Timber
import kotlin.system.exitProcess

class App : Application(), KoinComponent {
    fun scanReminderWorkRunner(): ScanReminderWorkRunner = get<ScanReminderWorkRunner>()

    fun scanReminderScheduler(): ScanReminderScheduler = get<ScanReminderScheduler>()

    fun notificationInboxRepository(): NotificationInboxRepository =
        get<NotificationInboxRepository>()

    override fun onCreate() {
        super.onCreate()
        installTerraSdkCrashShield()
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        else Timber.plant(CrashlyticsTree())
        
        installAppCheck()
        wipeFirestoreCacheIfPending()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
        TerraSdkSync.accessCheck = get<HealthDataGate>()::sdkLookbackDays
        trackSignedInUser()
        SystemNotificationHelper.createChannels(this)
        scanReminderScheduler().onNotificationSettingsOrScanChanged()
        registerActivityLifecycleCallbacks(KeepScreenOn)
        registerActivityLifecycleCallbacks(ScanCameraCover)
        registerActivityLifecycleCallbacks(get<CurrentActivityProvider>())
    }

    private fun trackSignedInUser() {
        val telemetry = get<Telemetry>()
        get<FirebaseAuth>().addAuthStateListener { auth ->
            telemetry.identify(auth.currentUser?.uid)
        }
    }

    private fun installAppCheck() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            },
        )
    }

    private fun wipeFirestoreCacheIfPending() {
        if (!PendingFirestoreCacheWipe.isArmed(this)) return
        FirebaseFirestore.getInstance().clearPersistence().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                PendingFirestoreCacheWipe.disarm(this)
                Timber.i("Cleared the Firestore cache left by the previous account")
            } else {
                Timber.w(task.exception, "Could not clear the Firestore cache; retrying next launch")
            }
        }
    }

    private fun installTerraSdkCrashShield() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (throwable.isTerraHealthConnectMissingUserIdCrash()) {
                Timber.e(throwable, "Swallowed Terra SDK Health Connect missing user_id crash")
                FirebaseCrashlytics.getInstance().recordException(throwable)
                return@setDefaultUncaughtExceptionHandler
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    private fun Throwable.isTerraHealthConnectMissingUserIdCrash(): Boolean {
        val rootCause = cause
        return this::class.java.name == "kotlinx.coroutines.CompletionHandlerException" &&
            rootCause is NullPointerException &&
            rootCause.stackTrace.any {
                it.className == "co.tryterra.terra.TerraManager" &&
                    it.methodName.contains("initHealthConnect")
            }
    }
}

private object ScanCameraCover : Application.ActivityLifecycleCallbacks {
    private val covered = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())

    override fun onActivityStarted(activity: Activity) {
        if (activity !is SdkActivity || !covered.add(activity)) return
        val cover = View(activity).apply { setBackgroundColor(Color.BLACK) }
        activity.addContentView(cover, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        cover.postDelayed(
            {
                cover.animate()
                    .alpha(0f)
                    .setDuration(AnimationConstants.SDK_CAMERA_COVER_FADE_MS)
                    .withEndAction { (cover.parent as? ViewGroup)?.removeView(cover) }
                    .start()
            },
            AnimationConstants.SDK_CAMERA_COVER_HOLD_MS,
        )
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

private object KeepScreenOn : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is SdkActivity)
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
