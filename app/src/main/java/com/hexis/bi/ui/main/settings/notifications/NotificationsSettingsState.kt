package com.hexis.bi.ui.main.settings.notifications

import com.hexis.bi.domain.enums.ReminderDay

private const val DEFAULT_REMINDER_HOUR = 14
private val DEFAULT_REMINDER_DAY = ReminderDay.Wednesday

data class NotificationsState(
    val notificationsEnabled: Boolean = false,
    val voiceEnabled: Boolean = false,
    val remindToScanEnabled: Boolean = false,
    val reminderDay: ReminderDay = DEFAULT_REMINDER_DAY,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val showDayPicker: Boolean = false,
    val showTimePicker: Boolean = false,
)
