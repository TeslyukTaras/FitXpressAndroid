package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.notification.NotificationInboxRepository
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.inchesToFeetAndInches
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

sealed interface HomeEvent : UiEvent {
    data object NavigateToLogin : HomeEvent
}

class HomeViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val suitRepository: SuitRepository,
    private val sleepRepository: SleepRepository,
    private val terraManagerHolder: TerraManagerHolder,
    private val notificationInbox: NotificationInboxRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(
        HomeState(
            sleepGoalHours = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
            overviewCards = buildOverviewCardsWithSleep(
                HomeOverviewDefaults.placeholderSleepCard(
                    appContext,
                    SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
                ),
            ),
        ),
    )
    val state = _state.asStateFlow()

    init {
        combine(
            userRepository.observeUser(),
            userRepository.observeUserSettings(),
        ) { profile, settings ->
            profile to settings
        }
            .onEach { (profile, settings) ->
                val isMetric = profile.unitSystem.isMetricUnitSystem()
                val goalHours = settings.sleepGoalHours ?: SleepConstants.DEFAULT_SLEEP_GOAL_HOURS
                _state.update { current ->
                    current.copy(
                        userName = "${profile.firstName} ${profile.lastName}".trim(),
                        avatarUrl = profile.avatarUrl,
                        weight = if (isMetric)
                            profile.weightKg?.let {
                                appContext.getString(R.string.unit_weight_kg, it)
                            }
                        else
                            profile.weightLb?.let {
                                appContext.getString(R.string.unit_weight_lb, it)
                            },
                        height = if (isMetric)
                            profile.heightCm?.let {
                                appContext.getString(R.string.unit_height_cm, it)
                            }
                        else
                            profile.heightIn?.let {
                                val (ft, inches) = it.inchesToFeetAndInches()
                                appContext.getString(R.string.unit_height_ft_in, ft, inches)
                            },
                        age = profile.dateOfBirth?.calculateAge()
                            ?.let { appContext.getString(R.string.unit_age_years, it) },
                        sleepGoalHours = goalHours,
                    )
                }
                patchSleepOverviewGoalSubtitle(goalHours)
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        suitRepository.connectionState
            .onEach { info ->
                _state.update { current ->
                    current.copy(isSuitConnected = info != null)
                }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)

        notificationInbox.unreadCount
            .onEach { count ->
                _state.update { it.copy(hasUnreadNotifications = count > 0) }
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }

    /**
     * Updates only the sleep card subtitle when [sleepGoalHours] changes from Firestore settings,
     * without re-hitting Terra REST for duration.
     */
    private fun patchSleepOverviewGoalSubtitle(goalHours: Int) {
        val subtitle = appContext.getString(R.string.home_sleep_goal, goalHours.toString())
        _state.update { s ->
            if (s.overviewCards.isEmpty()) return@update s
            val next = s.overviewCards.toMutableList()
            val first = next[0]
            if (first.subtitle == subtitle) return@update s
            next[0] = first.copy(subtitle = subtitle)
            s.copy(overviewCards = next)
        }
    }

    /**
     * Loads last night’s sleep duration for the overview card. Call when Home is shown so values
     * refresh after returning from Sleep or Health Connect sync.
     */
    fun refreshSleepOverview() {
        viewModelScope.launch {
            loadSleepOverview()
        }
    }

    private suspend fun loadSleepOverview() {
        terraManagerHolder.awaitCurrentOrTimeout()
        val goalHours = _state.value.sleepGoalHours
        val goalSubtitle = appContext.getString(R.string.home_sleep_goal, goalHours.toString())

        sleepRepository.getSessionForNight(LocalDate.now()).fold(
            onSuccess = { session ->
                val card = session.toSleepOverviewCard(goalSubtitle)
                _state.update { it.copy(overviewCards = buildOverviewCardsWithSleep(card)) }
            },
            onFailure = {
                val card = HomeOverviewDefaults.sleepCard(
                    context = appContext,
                    goalSubtitle = goalSubtitle,
                    value = appContext.getString(R.string.sleep_placeholder),
                    valueLabel = null,
                )
                _state.update { it.copy(overviewCards = buildOverviewCardsWithSleep(card)) }
            },
        )
    }

    private fun SleepSession?.toSleepOverviewCard(goalSubtitle: String): OverviewCardData {
        val placeholder = appContext.getString(R.string.sleep_placeholder)
        val hasDuration = this != null && durationMinutes > 0
        val valueText = if (hasDuration) {
            "%.1f".format(Locale.US, durationMinutes / 60f)
        } else {
            placeholder
        }
        return HomeOverviewDefaults.sleepCard(
            context = appContext,
            goalSubtitle = goalSubtitle,
            value = valueText,
            valueLabel = if (hasDuration) appContext.getString(R.string.unit_hours_short) else null,
        )
    }
}
