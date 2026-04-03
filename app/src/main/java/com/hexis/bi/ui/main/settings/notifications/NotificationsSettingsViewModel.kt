package com.hexis.bi.ui.main.settings.notifications

import android.app.Application
import com.hexis.bi.domain.enums.ReminderDay
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationsSettingsViewModel(
    application: Application,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(NotificationsState())
    val state = _state.asStateFlow()

    fun toggleNotifications(enabled: Boolean) =
        _state.update { it.copy(notificationsEnabled = enabled) }

    fun toggleVoice(enabled: Boolean) = _state.update { it.copy(voiceEnabled = enabled) }
    fun toggleRemindToScan(enabled: Boolean) =
        _state.update { it.copy(remindToScanEnabled = enabled) }

    fun showDayPicker() = _state.update { it.copy(showDayPicker = true) }
    fun hideDayPicker() = _state.update { it.copy(showDayPicker = false) }
    fun selectDay(day: ReminderDay) =
        _state.update { it.copy(reminderDay = day, showDayPicker = false) }

    fun showTimePicker() = _state.update { it.copy(showTimePicker = true) }
    fun hideTimePicker() = _state.update { it.copy(showTimePicker = false) }
    fun selectTime(hour: Int) =
        _state.update { it.copy(reminderHour = hour, showTimePicker = false) }
}
