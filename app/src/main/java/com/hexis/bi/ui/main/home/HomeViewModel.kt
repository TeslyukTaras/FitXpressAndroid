package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.sleep.SleepRepository
import com.hexis.bi.data.sleep.SleepSession
import com.hexis.bi.data.terra.TerraManagerHolder
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.suit.SuitRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.utils.calculateAge
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.SleepConstants
import com.hexis.bi.utils.inchesToFeetAndInches
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.async
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
    private val activityRepository: ActivityRepository,
    private val terraManagerHolder: TerraManagerHolder,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(
        HomeState(
            sleepGoalHours = SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
            activityGoalSteps = ActivityConstants.DEFAULT_STEP_GOAL,
            overviewCards = buildOverviewCards(
                HomeOverviewDefaults.placeholderSleepCard(
                    appContext,
                    SleepConstants.DEFAULT_SLEEP_GOAL_HOURS,
                ),
                HomeOverviewDefaults.placeholderActivityCard(
                    appContext,
                    ActivityConstants.DEFAULT_STEP_GOAL,
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
                val activityGoalSteps = settings.stepsGoal ?: ActivityConstants.DEFAULT_STEP_GOAL
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
                        activityGoalSteps = activityGoalSteps,
                    )
                }
                patchSleepOverviewGoalSubtitle(goalHours)
                patchActivityOverviewGoalSubtitle(activityGoalSteps)
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
    }

    /**
     * Updates only the sleep card subtitle when [sleepGoalHours] changes from Firestore settings,
     * without re-hitting Terra REST for duration.
     */
    private fun patchSleepOverviewGoalSubtitle(goalHours: Int) {
        val subtitle = appContext.getString(R.string.home_sleep_goal, goalHours.toString())
        _state.update { s ->
            val current = s.overviewCards.getOrNull(OVERVIEW_SLEEP_INDEX) ?: return@update s
            if (current.subtitle == subtitle) return@update s
            s.copy(overviewCards = s.overviewCards.replaced(OVERVIEW_SLEEP_INDEX, current.copy(subtitle = subtitle)))
        }
    }

    /**
     * Updates only the activity card subtitle when [activityGoalSteps] changes from Firestore
     * settings, without re-hitting Terra REST for today's steps.
     */
    private fun patchActivityOverviewGoalSubtitle(activityGoalSteps: Int) {
        val subtitle = appContext.getString(
            R.string.home_activity_goal,
            HomeOverviewDefaults.formatSteps(activityGoalSteps),
        )
        _state.update { s ->
            val current = s.overviewCards.getOrNull(OVERVIEW_ACTIVITY_INDEX) ?: return@update s
            if (current.subtitle == subtitle) return@update s
            s.copy(overviewCards = s.overviewCards.replaced(OVERVIEW_ACTIVITY_INDEX, current.copy(subtitle = subtitle)))
        }
    }

    /**
     * Loads last night's sleep duration and today's activity for the overview cards. Call when Home
     * is shown so values refresh after returning from Sleep / Activity / Health Connect sync.
     */
    fun refreshOverview() {
        viewModelScope.launch {
            terraManagerHolder.awaitCurrentOrTimeout()
            val sleepJob = async { loadSleepOverview() }
            val activityJob = async { loadActivityOverview() }
            sleepJob.await()
            activityJob.await()
        }
    }

    private suspend fun loadSleepOverview() {
        val goalHours = _state.value.sleepGoalHours
        val goalSubtitle = appContext.getString(R.string.home_sleep_goal, goalHours.toString())

        sleepRepository.getSessionForNight(LocalDate.now()).fold(
            onSuccess = { session ->
                val card = session.toSleepOverviewCard(goalSubtitle)
                _state.update { it.copy(overviewCards = it.overviewCards.replaced(OVERVIEW_SLEEP_INDEX, card)) }
            },
            onFailure = {
                val card = HomeOverviewDefaults.sleepCard(
                    context = appContext,
                    goalSubtitle = goalSubtitle,
                    value = appContext.getString(R.string.sleep_placeholder),
                    valueLabel = null,
                )
                _state.update { it.copy(overviewCards = it.overviewCards.replaced(OVERVIEW_SLEEP_INDEX, card)) }
            },
        )
    }

    private suspend fun loadActivityOverview() {
        val stepsGoal = _state.value.activityGoalSteps
        val goalSubtitle = appContext.getString(
            R.string.home_activity_goal,
            HomeOverviewDefaults.formatSteps(stepsGoal),
        )

        activityRepository.getSummaryForDate(LocalDate.now()).fold(
            onSuccess = { summary ->
                val card = summary.toActivityOverviewCard(goalSubtitle)
                _state.update { it.copy(overviewCards = it.overviewCards.replaced(OVERVIEW_ACTIVITY_INDEX, card)) }
            },
            onFailure = {
                val card = HomeOverviewDefaults.activityCard(
                    context = appContext,
                    goalSubtitle = goalSubtitle,
                    value = appContext.getString(R.string.sleep_placeholder),
                    valueLabel = null,
                )
                _state.update { it.copy(overviewCards = it.overviewCards.replaced(OVERVIEW_ACTIVITY_INDEX, card)) }
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

    private fun ActivitySummary?.toActivityOverviewCard(goalSubtitle: String): OverviewCardData {
        val placeholder = appContext.getString(R.string.sleep_placeholder)
        val hasSteps = this != null && steps > 0
        val valueText = if (hasSteps) HomeOverviewDefaults.formatSteps(steps) else placeholder
        return HomeOverviewDefaults.activityCard(
            context = appContext,
            goalSubtitle = goalSubtitle,
            value = valueText,
            valueLabel = if (hasSteps) appContext.getString(R.string.home_activity_value_label) else null,
        )
    }
}

private fun List<OverviewCardData>.replaced(index: Int, card: OverviewCardData): List<OverviewCardData> {
    if (index !in indices) return this
    return toMutableList().also { it[index] = card }
}
