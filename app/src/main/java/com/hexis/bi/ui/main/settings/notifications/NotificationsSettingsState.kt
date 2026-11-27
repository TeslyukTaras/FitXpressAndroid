package com.hexis.bi.ui.main.settings.notifications

import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.utils.constants.ScanReminderRules
import java.time.LocalDate

data class NotificationsState(
    val settingsLoaded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val voiceEnabled: Boolean = true,
    val remindToScanEnabled: Boolean = false,
    val nextReminderDate: LocalDate? = null,
    val reminderHour: Int = ScanReminderRules.DEFAULT_HOUR,
    val dateRange: ClosedRange<LocalDate>? = null,
    val baselineScanMillis: Long = 0L,
    val showDatePicker: Boolean = false,
    val showTimePicker: Boolean = false,
) {
    fun mergedWithUserSettings(
        settings: UserSettings,
    ): NotificationsState = copy(
        settingsLoaded = true,
        notificationsEnabled = settings.pushNotificationsEnabled ?: false,
        voiceEnabled = settings.voiceGuidanceEnabled ?: true,
        remindToScanEnabled = settings.scanRemindersEnabled ?: false,
    )
}
