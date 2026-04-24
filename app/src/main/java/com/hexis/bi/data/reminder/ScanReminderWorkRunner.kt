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
import com.hexis.bi.data.scan.ScanHistoryRepository
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.domain.scan.ScanReminderPolicy
import com.hexis.bi.utils.SystemNotificationHelper
import com.hexis.bi.utils.constants.ScanReminderAlarms
import com.hexis.bi.utils.constants.SystemTrayNotificationId
import timber.log.Timber
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

class ScanReminderWorkRunner(
    private val app: Application,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val preferences: UserPreferencesRepository,
    private val scheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
) {
    suspend fun run(kind: String) {
        try {
            if (auth.currentUser == null) return
            val settings = userRepository.getUserSettings().getOrElse {
                Timber.w(it, "ScanReminderWorkRunner($kind): getUserSettings failed")
                return
            }
            if (settings.pushNotificationsEnabled != true) return
            if (settings.scanRemindersEnabled != true) return

            val now = ZonedDateTime.now(ZoneId.systemDefault())
            when (kind) {
                ScanReminderAlarms.KIND_TODAY -> runToday(now, settings)
                ScanReminderAlarms.KIND_NUDGE -> runNudge(now, settings)
                ScanReminderAlarms.KIND_MISSED -> runMissed(now, settings)
                else -> Timber.w("ScanReminderWorkRunner: unknown kind=%s", kind)
            }
        } finally {
            // Always re-schedule the next cycle (or cancel it) after firing, even on failure.
            scheduler.onNotificationSettingsOrScanChanged()
        }
    }

    private suspend fun runToday(now: ZonedDateTime, settings: UserSettings) {
        val ctxToday = ScanReminderPolicy.todayContextFor(now, settings) ?: return

        val week = ScanReminderPolicy.weekKeyForNudge(ctxToday.todayZoned)
        if (week == preferences.getLastScanTodayIsoWeek()) return

        if (scanAlreadySavedInCycle(ctxToday.startOfScanDay, ctxToday.todayZoned)) return

        // In-app history: always, so the message list is complete even if tray is disabled / denied.
        notificationInbox.appendInbox(
            R.string.notif_scan_today_title,
            R.string.notif_scan_today_body,
        )
        preferences.setLastScanTodayIsoWeek(week)

        if (canPostSystemNotification()) {
            SystemNotificationHelper.showBodyScanReminder(
                app,
                SystemTrayNotificationId.BODY_SCAN_TODAY,
                app.getString(R.string.notif_scan_today_title),
                app.getString(R.string.notif_scan_today_body),
            )
        }
    }

    private suspend fun runNudge(now: ZonedDateTime, settings: UserSettings) {
        val ctxNudge = ScanReminderPolicy.nudgeContextFor(now, settings) ?: return

        val week = ScanReminderPolicy.weekKeyForNudge(ctxNudge.nudgeZoned)
        if (week == preferences.getLastScanNudgeIsoWeek()) return

        if (scanAlreadySavedInCycle(ctxNudge.startOfScanDay, ctxNudge.nudgeZoned)) return

        notificationInbox.appendInbox(
            R.string.notif_finish_scan_today_title,
            R.string.notif_finish_scan_today_body,
        )
        preferences.setLastScanNudgeIsoWeek(week)

        if (canPostSystemNotification()) SystemNotificationHelper.showBodyScanReminder(
            app,
            SystemTrayNotificationId.BODY_SCAN_FINISH_TODAY,
            app.getString(R.string.notif_finish_scan_today_title),
            app.getString(R.string.notif_finish_scan_today_body),
        )
    }

    private suspend fun runMissed(now: ZonedDateTime, settings: UserSettings) {
        val ctxMissed = ScanReminderPolicy.missedContextFor(now, settings) ?: return

        val week = ScanReminderPolicy.weekKeyForNudge(ctxMissed.missedZoned)
        if (week == preferences.getLastScanMissedIsoWeek()) return

        if (scanAlreadySavedInCycle(ctxMissed.startOfScanDay, ctxMissed.missedZoned)) return

        preferences.setLastScanMissedIsoWeek(week)
        notificationInbox.appendInbox(
            R.string.notif_missed_title,
            R.string.notif_missed_body,
            bodyFormatArg = scheduledDayLabel(settings),
        )
    }

    private fun scheduledDayLabel(settings: UserSettings): String? {
        val name = settings.reminderDay ?: return null
        return runCatching {
            DayOfWeek.valueOf(name.uppercase(Locale.ROOT))
                .getDisplayName(TextStyle.FULL, Locale.getDefault())
        }.getOrNull()
    }

    private suspend fun scanAlreadySavedInCycle(
        start: ZonedDateTime,
        end: ZonedDateTime,
    ): Boolean {
        val startMs = start.toInstant().toEpochMilli()
        val endMs = end.toInstant().toEpochMilli()
        return scanHistoryRepository
            .hasScanSavedBetween(startMs, endMs)
            .getOrElse { false }
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
