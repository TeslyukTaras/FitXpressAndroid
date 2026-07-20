package com.hexis.bi

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Process
import android.view.WindowManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.reminder.ScanReminderWorkRunner
import com.hexis.bi.di.appModule
import com.hexis.bi.utils.CrashlyticsTree
import com.hexis.bi.utils.SystemNotificationHelper
import com.look.camera.sdk.SdkActivity
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
        
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
        SystemNotificationHelper.createChannels(this)
        scanReminderScheduler().onNotificationSettingsOrScanChanged()
        registerActivityLifecycleCallbacks(KeepScreenOn)
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
