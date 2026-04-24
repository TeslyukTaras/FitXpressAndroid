package com.hexis.bi.data.reminder

/**
 * Schedules [android.app.AlarmManager] for local body-scan reminders
 * (today, next-day nudge, missed) so they can fire with the app process not running.
 *
 * Call [onNotificationSettingsOrScanChanged] when user settings change or a scan is saved; it
 * re-computes the next instants and updates alarms.
 */
interface ScanReminderScheduler {
    fun onNotificationSettingsOrScanChanged()
}
