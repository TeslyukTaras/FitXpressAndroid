package com.hexis.bi.ui.main.settings.notifications

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.preferences.UserPreferencesRepository
import com.hexis.bi.data.reminder.ScanReminderResolver
import com.hexis.bi.data.reminder.ScanReminderScheduler
import com.hexis.bi.data.subscription.SubscriptionRepository
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.data.user.UserSettings
import com.hexis.bi.ui.base.BaseViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update

class NotificationsSettingsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
    private val scanReminderScheduler: ScanReminderScheduler,
    private val notificationInbox: NotificationInboxRepository,
    private val resolver: ScanReminderResolver,
    private val subscriptionRepository: SubscriptionRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(NotificationsState())
    val state = _state.asStateFlow()

    private var latestSettings: UserSettings? = null
    private var forecastKey: List<Any?>? = null

    init {
        launch(showLoading = false) {
            preferences.ensureVoiceMigratedToFirestore(userRepository)
        }
        launch(showLoading = false) {
            userRepository.observeUserSettings().collectLatest { settings ->
                latestSettings = settings
                _state.update { prev -> prev.mergedWithUserSettings(settings) }
                val key = settings.forecastKey()
                if (key != forecastKey) {
                    forecastKey = key
                    refreshForecast(settings)
                }
            }
        }
        launch(showLoading = false) {
            subscriptionRepository.status.drop(1).collect {
                latestSettings?.let { refreshForecast(it) }
            }
        }
    }

    private fun UserSettings.forecastKey(): List<Any?> =
        listOf(reminderHour, nextReminderDate, nextReminderHour, nextReminderScanMillis)

    private suspend fun refreshForecast(settings: UserSettings) {
        val forecast = resolver.forecast(ZonedDateTime.now(ZoneId.systemDefault()), settings)
        if (forecast == null) {
            forecastKey = null
            _state.update { it.copy(dateRange = null) }
            return
        }
        _state.update {
            it.copy(
                nextReminderDate = forecast.next?.at?.toLocalDate(),
                reminderHour = forecast.next?.at?.hour ?: forecast.nextHour,
                dateRange = forecast.pickerRange,
                baselineScanMillis = forecast.baselineScanMillis,
            )
        }
    }

    private fun updateSettings(
        vararg pairs: Pair<String, Any?>,
        rescheduleScanReminders: Boolean = true,
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
    }

    fun toggleNotifications(enabled: Boolean) = updateSettings(
        FirestoreSchema.UserSettingsFields.PUSH_NOTIFICATIONS_ENABLED to enabled,
    )

    fun toggleVoice(enabled: Boolean) = updateSettings(
        FirestoreSchema.UserSettingsFields.VOICE_GUIDANCE_ENABLED to enabled,
        rescheduleScanReminders = false,
    )

    fun toggleRemindToScan(enabled: Boolean) = launch(showLoading = false) {
        val result = runCatching {
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.SCAN_REMINDERS_ENABLED to enabled),
            )
        }
        if (result.isFailure) {
            setError(result.exceptionOrNull()?.message ?: "Could not save settings")
            return@launch
        }
        scanReminderScheduler.onNotificationSettingsOrScanChanged()
        if (enabled) announceNextReminder()
    }

    fun showDatePicker() = _state.update { it.copy(showDatePicker = true) }
    fun hideDatePicker() = _state.update { it.copy(showDatePicker = false) }
    fun selectDate(date: LocalDate) {
        _state.update { it.copy(showDatePicker = false) }
        if (_state.value.dateRange == null) return
        saveNextReminder(date, _state.value.reminderHour)
    }

    fun showTimePicker() = _state.update { it.copy(showTimePicker = true) }
    fun hideTimePicker() = _state.update { it.copy(showTimePicker = false) }
    fun selectTime(hour: Int) {
        _state.update { it.copy(showTimePicker = false) }
        if (_state.value.dateRange == null) return
        val date = _state.value.nextReminderDate ?: return
        saveNextReminder(date, hour)
    }

    private fun saveNextReminder(date: LocalDate, hour: Int) = launch(showLoading = false) {
        val result = runCatching {
            userRepository.updateUserSettings(
                mapOf(
                    FirestoreSchema.UserSettingsFields.NEXT_REMINDER_DATE to date.toString(),
                    FirestoreSchema.UserSettingsFields.NEXT_REMINDER_HOUR to hour,
                    FirestoreSchema.UserSettingsFields.NEXT_REMINDER_SCAN_MILLIS to _state.value.baselineScanMillis,
                ),
            )
        }
        if (result.isFailure) {
            setError(result.exceptionOrNull()?.message ?: "Could not save settings")
            return@launch
        }
        scanReminderScheduler.onNotificationSettingsOrScanChanged()
        if (_state.value.remindToScanEnabled) announceNextReminder(date)
    }

    private suspend fun announceNextReminder(date: LocalDate? = _state.value.nextReminderDate) {
        val label = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: return
        notificationInbox.appendNextScheduledScanInboxDeduped(label)
    }

    fun notifyNotificationsPermissionDenied() {
        setError(R.string.error_notifications_permission_denied)
    }
}
