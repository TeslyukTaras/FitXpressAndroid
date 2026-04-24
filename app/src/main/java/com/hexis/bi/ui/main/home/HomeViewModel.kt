package com.hexis.bi.ui.main.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.R
import com.hexis.bi.data.sleep.TerraSleepRepository
import com.hexis.bi.data.sleep.TerraSleepSession
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
    private val terraSleepRepository: TerraSleepRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        userRepository.observeUser()
            .onEach { profile ->
                val isMetric = profile.unitSystem.isMetricUnitSystem()
                _state.update { current ->
                    current.copy(
                        userName = "${profile.firstName} ${profile.lastName}".trim(),
                        avatarUrl = profile.avatarUrl,
                        weight = if (isMetric)
                            profile.weightKg?.let {
                                appContext.getString(
                                    R.string.unit_weight_kg,
                                    it
                                )
                            }
                        else
                            profile.weightLb?.let {
                                appContext.getString(
                                    R.string.unit_weight_lb,
                                    it
                                )
                            },
                        height = if (isMetric)
                            profile.heightCm?.let {
                                appContext.getString(
                                    R.string.unit_height_cm,
                                    it
                                )
                            }
                        else
                            profile.heightIn?.let {
                                val (ft, inches) = it.inchesToFeetAndInches()
                                appContext.getString(
                                    R.string.unit_height_ft_in,
                                    ft,
                                    inches
                                )
                            },
                        age = profile.dateOfBirth?.calculateAge()
                            ?.let { appContext.getString(R.string.unit_age_years, it) },
                    )
                }
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
     * Loads last night’s sleep duration for the overview card. Call when Home is shown so values
     * refresh after returning from Sleep or Health Connect sync.
     */
    fun refreshSleepOverview() {
        viewModelScope.launch {
            loadSleepOverview()
        }
    }

    private suspend fun loadSleepOverview() {
        val goalHours = userRepository.getUserSettings().getOrNull()?.sleepGoalHours
            ?: SleepConstants.DEFAULT_SLEEP_GOAL_HOURS
        val goalSubtitle = appContext.getString(R.string.home_sleep_goal, goalHours.toString())

        terraSleepRepository.getSessionForNight(LocalDate.now()).fold(
            onSuccess = { session ->
                val card = session.toSleepOverviewCard(goalSubtitle)
                _state.update { it.copy(overviewCards = buildOverviewCardsWithSleep(card)) }
            },
            onFailure = {
                val card = OverviewCardData(
                    title = appContext.getString(R.string.home_card_sleep),
                    iconRes = R.drawable.ic_moon,
                    value = appContext.getString(R.string.sleep_placeholder),
                    valueLabel = null,
                    subtitle = goalSubtitle,
                    variant = OverviewCardVariant.Accent,
                )
                _state.update { it.copy(overviewCards = buildOverviewCardsWithSleep(card)) }
            },
        )
    }

    private fun TerraSleepSession?.toSleepOverviewCard(goalSubtitle: String): OverviewCardData {
        val placeholder = appContext.getString(R.string.sleep_placeholder)
        val hasDuration = this != null && durationMinutes > 0
        val valueText = if (hasDuration) {
            "%.1f".format(Locale.US, durationMinutes / 60f)
        } else {
            placeholder
        }
        return OverviewCardData(
            title = appContext.getString(R.string.home_card_sleep),
            iconRes = R.drawable.ic_moon,
            value = valueText,
            valueLabel = if (hasDuration) "h" else null,
            subtitle = goalSubtitle,
            variant = OverviewCardVariant.Accent,
        )
    }
}
