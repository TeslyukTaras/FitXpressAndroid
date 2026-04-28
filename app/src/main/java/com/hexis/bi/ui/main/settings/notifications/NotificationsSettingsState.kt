package com.hexis.bi.ui.main.settings.notifications

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.domain.enums.ReminderDay

const val DEFAULT_REMINDER_HOUR = 14
val DEFAULT_REMINDER_DAY = ReminderDay.Wednesday

@StringRes
fun ReminderDay.labelRes(): Int = when (this) {
    ReminderDay.Monday -> R.string.day_monday
    ReminderDay.Tuesday -> R.string.day_tuesday
    ReminderDay.Wednesday -> R.string.day_wednesday
    ReminderDay.Thursday -> R.string.day_thursday
    ReminderDay.Friday -> R.string.day_friday
    ReminderDay.Saturday -> R.string.day_saturday
    ReminderDay.Sunday -> R.string.day_sunday
}

data class NotificationsState(
    val settingsLoaded: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val voiceEnabled: Boolean = true,
    val remindToScanEnabled: Boolean = false,
    val reminderDay: ReminderDay = DEFAULT_REMINDER_DAY,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val showDayPicker: Boolean = false,
    val showTimePicker: Boolean = false,
) {
    fun mergedWithUserSettings(
        settings: UserSettings,
    ): NotificationsState {
        val day = ReminderDay.entries
            .find { it.name == settings.reminderDay } ?: DEFAULT_REMINDER_DAY
        return copy(
            settingsLoaded = true,
            notificationsEnabled = settings.pushNotificationsEnabled ?: false,
            voiceEnabled = settings.voiceGuidanceEnabled ?: true,
            remindToScanEnabled = settings.scanRemindersEnabled ?: false,
            reminderDay = day,
            reminderHour = settings.reminderHour ?: DEFAULT_REMINDER_HOUR,
        )
    }
}
