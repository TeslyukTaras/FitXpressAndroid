package com.hexis.bi.ui.main.home.activity

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.caloriesGoal
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.distanceGoalKm
import com.hexis.bi.utils.formatFullMonthDay
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlin.random.Random

class ActivityViewModel(
    application: Application,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var dayOffset = 0
    private var weightKg = ProfileConstants.DEFAULT_WEIGHT_KG

    init {
        loadDayData(0)
        userRepository.observeUser()
            .onEach { profile ->
                val heightCm = profile.heightCm?.toFloat()
                val isFemale = profile.gender?.lowercase() == "female"
                val stepGoal = _state.value.stepsGoal

                profile.weightKg?.toFloat()?.let { weightKg = it }

                val distGoalKm = if (heightCm != null)
                    distanceGoalKm(stepGoal, heightCm, isFemale)
                else ActivityConstants.DEFAULT_DISTANCE_GOAL_KM

                val calGoal = if (distGoalKm > 0f && weightKg > 0f)
                    caloriesGoal(distGoalKm, weightKg)
                else ActivityConstants.DEFAULT_CALORIES_GOAL

                _state.update {
                    it.copy(
                        isMetric = profile.unitSystem.isMetricUnitSystem(),
                        distanceGoalKm = distGoalKm,
                        caloriesGoal = calGoal,
                    )
                }
                loadDayData(dayOffset)
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }

    fun selectTab(tab: ActivityTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun previousDay() {
        dayOffset--
        loadDayData(dayOffset)
    }

    fun nextDay() {
        if (dayOffset < 0) {
            dayOffset++
            loadDayData(dayOffset)
        }
    }

    fun showInfoSheet() {
        _state.update { it.copy(showInfoSheet = true) }
    }

    fun dismissInfoSheet() {
        _state.update { it.copy(showInfoSheet = false) }
    }

    private fun randomGoalFraction(random: Random): Float =
        ActivityConstants.MOCK_GOAL_FRACTION_MIN +
                random.nextFloat() *
                (ActivityConstants.MOCK_GOAL_FRACTION_MAX - ActivityConstants.MOCK_GOAL_FRACTION_MIN)

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        val random = Random(day.toEpochDay())

        val hourlySteps = (0 until ActivityConstants.HOURS_IN_DAY).map { hour ->
            val steps = random.nextInt(0, 600)
            HourlyStepEntry(hour = hour, steps = steps)
        }

        val totalSteps = hourlySteps.sumOf { it.steps }
        val current = _state.value
        val distanceKm = (randomGoalFraction(random) * current.distanceGoalKm)
            .let { (it * 10).toInt() / 10f }
        val calories = (randomGoalFraction(random) * current.caloriesGoal).toInt()

        _state.update {
            it.copy(
                dateLabel = day.formatFullMonthDay(),
                currentSteps = totalSteps,
                calories = calories,
                distanceKm = distanceKm,
                hourlySteps = hourlySteps,
                canGoNextDay = offset < 0,
            )
        }
    }
}
