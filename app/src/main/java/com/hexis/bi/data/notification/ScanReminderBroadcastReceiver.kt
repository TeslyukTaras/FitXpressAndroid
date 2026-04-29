package com.hexis.bi.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hexis.bi.App
import com.hexis.bi.utils.constants.ScanReminderAlarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * One entry for:
 * 1. AlarmManager one-shot `PendingIntent`s (explicit intent with ScanReminderAlarms.BROADCAST_INTENT_EXTRA_KIND)
 * 2. System events that require re-arming (boot, time / zone change, app update) → ScanReminderScheduler
 */
class ScanReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val app = context.applicationContext as? App ?: return

        val kind = intent.getStringExtra(ScanReminderAlarms.BROADCAST_INTENT_EXTRA_KIND)
        if (!kind.isNullOrBlank()) {
            val pending = goAsync()
            try {
                runBlocking(Dispatchers.Default) {
                    try {
                        app.scanReminderWorkRunner().run(kind)
                    } catch (e: Exception) {
                        Timber.w(
                            e,
                            "ScanReminderBroadcastReceiver: run failed kind=%s",
                            kind
                        )
                    }
                }
            } finally {
                pending.finish()
            }
            return
        }

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
                -> app.scanReminderScheduler().onNotificationSettingsOrScanChanged()
        }
    }
}