package com.hexis.bi.ui.main.settings.notifications

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.enums.ReminderDay
import com.hexis.bi.ui.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NotificationsSettingsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
    private val scanReminderScheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(NotificationsState())
    val state = _state.asStateFlow()

    init {
        launch(showLoading = false) {
            preferences.ensureVoiceMigratedToFirestore(userRepository)
        }
        launch(showLoading = false) {
            userRepository.observeUserSettings().collect { settings ->
                _state.update { prev ->
                    prev
                        .mergedWithUserSettings(settings)
                        .copy(
                            showDayPicker = prev.showDayPicker,
                            showTimePicker = prev.showTimePicker,
                        )
                }
            }
        }
    }

    private fun updateSettings(
        rescheduleScanReminders: Boolean = true,
        appendNextScanInbox: Boolean = false,
        vararg pairs: Pair<String, Any?>,
    ) = launch(showLoading = false) {
        val result = runCatching {
            userRepository.updateUserSettings(mapOf(*pairs))
        }
        if (result.isFailure) {
            setError(result.exceptionOrNull()?.message ?: "Could not save settings")
            return@launch
        }
        if (rescheduleScanReminders) {
            scanReminderScheduler.onNotificationSettingsOrScanChanged()
        }
        if (appendNextScanInbox && _state.value.remindToScanEnabled) {
            val day = _state.value.reminderDay
            notificationInbox.appendNextScheduledScanInboxDeduped(
                appContext.getString(day.labelRes()),
            )
        }
    }

    fun toggleNotifications(enabled: Boolean) = updateSettings(
        rescheduleScanReminders = true,
        appendNextScanInbox = false,
        FirestoreSchema.UserSettingsFields.PUSH_NOTIFICATIONS_ENABLED to enabled,
    )

    fun toggleVoice(enabled: Boolean) = updateSettings(
        rescheduleScanReminders = false,
        appendNextScanInbox = false,
        FirestoreSchema.UserSettingsFields.VOICE_GUIDANCE_ENABLED to enabled,
    )

    fun toggleRemindToScan(enabled: Boolean) = launch(showLoading = false) {
        val snapshot = _state.value
        val updates = if (enabled) {
            mapOf(
                FirestoreSchema.UserSettingsFields.SCAN_REMINDERS_ENABLED to true,
                FirestoreSchema.UserSettingsFields.REMINDER_DAY to snapshot.reminderDay.name,
                FirestoreSchema.UserSettingsFields.REMINDER_HOUR to snapshot.reminderHour,
            )
        } else {
            mapOf(FirestoreSchema.UserSettingsFields.SCAN_REMINDERS_ENABLED to false)
        }
        val result = runCatching { userRepository.updateUserSettings(updates) }
        if (result.isFailure) {
            setError(result.exceptionOrNull()?.message ?: "Could not save settings")
            return@launch
        }
        scanReminderScheduler.onNotificationSettingsOrScanChanged()
        if (enabled) {
            val day = snapshot.reminderDay
            notificationInbox.appendNextScheduledScanInboxDeduped(
                appContext.getString(day.labelRes()),
            )
        }
    }

    fun showDayPicker() = _state.update { it.copy(showDayPicker = true) }
    fun hideDayPicker() = _state.update { it.copy(showDayPicker = false) }
    fun selectDay(day: ReminderDay) {
        _state.update { it.copy(showDayPicker = false) }
        launch(showLoading = false) {
            val result = runCatching {
                userRepository.updateUserSettings(
                    mapOf(FirestoreSchema.UserSettingsFields.REMINDER_DAY to day.name),
                )
            }
            if (result.isFailure) {
                setError(result.exceptionOrNull()?.message ?: "Could not save settings")
                return@launch
            }
            scanReminderScheduler.onNotificationSettingsOrScanChanged()
            if (_state.value.remindToScanEnabled) {
                notificationInbox.appendNextScheduledScanInboxDeduped(
                    appContext.getString(day.labelRes()),
                )
            }
        }
    }

    fun showTimePicker() = _state.update { it.copy(showTimePicker = true) }
    fun hideTimePicker() = _state.update { it.copy(showTimePicker = false) }
    fun selectTime(hour: Int) {
        _state.update { it.copy(showTimePicker = false) }
        updateSettings(
            rescheduleScanReminders = true,
            appendNextScanInbox = true,
            FirestoreSchema.UserSettingsFields.REMINDER_HOUR to hour,
        )
    }

    /** Surfaced by the UI when the POST_NOTIFICATIONS permission prompt is denied. */
    fun notifyNotificationsPermissionDenied() {
        setError(R.string.error_notifications_permission_denied)
    }
}
