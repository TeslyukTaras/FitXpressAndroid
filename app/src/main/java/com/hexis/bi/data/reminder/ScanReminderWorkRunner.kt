package com.hexis.bi.data.reminder

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.hexis.bi.R
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.scan.ReminderEvent
import com.hexis.bi.domain.scan.ReminderKind
import com.hexis.bi.domain.scan.SentReminders
import com.hexis.bi.utils.SystemNotificationHelper
import com.hexis.bi.utils.constants.ScanReminderRules
import com.hexis.bi.utils.constants.ScanReminderAlarms
import com.hexis.bi.utils.constants.SystemTrayNotificationId
import timber.log.Timber
import java.time.ZoneId
import java.time.ZonedDateTime

class ScanReminderWorkRunner(
    private val app: Application,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val preferences: UserPreferencesRepository,
    private val scheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
    private val resolver: ScanReminderResolver,
) {
    suspend fun run(kind: String) {
        try {
            if (auth.currentUser == null) return
            val reminderKind = reminderKindOf(kind) ?: run {
                Timber.w("ScanReminderWorkRunner: unknown kind=%s", kind)
                return
            }
            val settings = userRepository.getUserSettings().getOrElse {
                Timber.w(it, "ScanReminderWorkRunner($kind): getUserSettings failed")
                return
            }
            if (settings.pushNotificationsEnabled != true) return
            if (settings.scanRemindersEnabled != true) return

            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val forecast = resolver.forecast(now, settings) ?: return
            val due = forecast.events.firstOrNull {
                it.kind == reminderKind &&
                    !it.at.isAfter(now.plusMinutes(ScanReminderRules.FIRE_TOLERANCE_MINUTES))
            } ?: return

            when (reminderKind) {
                ReminderKind.TODAY -> runToday(due, now)
                ReminderKind.NUDGE -> runNudge(due)
                ReminderKind.MISSED -> runMissed(due)
            }
        } finally {
            scheduler.onNotificationSettingsOrScanChanged()
        }
    }

    private suspend fun runToday(event: ReminderEvent, now: ZonedDateTime) {
        val free = event.key.startsWith(ScanReminderRules.KEY_FREE)
        val title = if (free) R.string.notif_free_scan_title else R.string.notif_scan_today_title
        val body = if (free) R.string.notif_free_scan_body else R.string.notif_scan_today_body

        notificationInbox.appendInbox(title, body)
        preferences.setScanReminderTodaySent(SentReminders.encodeToday(event.key, now.toLocalDate()))

        if (canPostSystemNotification()) {
            SystemNotificationHelper.showBodyScanReminder(
                app,
                SystemTrayNotificationId.BODY_SCAN_TODAY,
                app.getString(title),
                app.getString(body),
            )
        }
    }

    private suspend fun runNudge(event: ReminderEvent) {
        notificationInbox.appendInbox(
            R.string.notif_finish_scan_today_title,
            R.string.notif_finish_scan_today_body,
        )
        preferences.setScanReminderNudgeSent(event.key)

        if (canPostSystemNotification()) SystemNotificationHelper.showBodyScanReminder(
            app,
            SystemTrayNotificationId.BODY_SCAN_FINISH_TODAY,
            app.getString(R.string.notif_finish_scan_today_title),
            app.getString(R.string.notif_finish_scan_today_body),
        )
    }

    private suspend fun runMissed(event: ReminderEvent) {
        preferences.setScanReminderMissedSent(event.key)
        notificationInbox.appendInbox(
            R.string.notif_missed_title,
            R.string.notif_missed_body,
        )
    }

    private fun reminderKindOf(kind: String): ReminderKind? = when (kind) {
        ScanReminderAlarms.KIND_TODAY -> ReminderKind.TODAY
        ScanReminderAlarms.KIND_NUDGE -> ReminderKind.NUDGE
        ScanReminderAlarms.KIND_MISSED -> ReminderKind.MISSED
        else -> null
    }

    private fun canPostSystemNotification(): Boolean {
        if (Build.VERSION.SDK_INT < 33)
            return NotificationManagerCompat.from(app).areNotificationsEnabled()
        val granted = ActivityCompat.checkSelfPermission(
            app,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        return NotificationManagerCompat.from(app).areNotificationsEnabled()
    }
}
