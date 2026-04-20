package com.hexis.bi.ui.main.home.activity

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.caloriesGoal
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.distanceGoalKm
import com.hexis.bi.utils.formatDayOfMonth
import com.hexis.bi.utils.formatFullMonthDay
import com.hexis.bi.utils.formatFullMonthYear
import com.hexis.bi.utils.formatHour
import com.hexis.bi.utils.formatMonthShort
import com.hexis.bi.utils.formatShortMonthDay
import com.hexis.bi.utils.formatYear
import com.hexis.bi.utils.isMetricUnitSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs
import kotlin.random.Random

class ActivityViewModel(
    application: Application,
    private val userRepository: UserRepository,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var dayOffset = 0
    private var weekOffset = 0
    private var monthOffset = 0
    private var yearOffset = 0
    private var weightKg = ProfileConstants.DEFAULT_WEIGHT_KG

    init {
        reloadAllTabs()
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
                reloadAllTabs()
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

    fun previousWeek() {
        weekOffset--
        loadWeekData(weekOffset)
    }

    fun nextWeek() {
        if (weekOffset < 0) {
            weekOffset++
            loadWeekData(weekOffset)
        }
    }

    fun previousMonth() {
        monthOffset--
        loadMonthData(monthOffset)
    }

    fun nextMonth() {
        if (monthOffset < 0) {
            monthOffset++
            loadMonthData(monthOffset)
        }
    }

    fun previousYear() {
        yearOffset--
        loadYearData(yearOffset)
    }

    fun nextYear() {
        if (yearOffset < 0) {
            yearOffset++
            loadYearData(yearOffset)
        }
    }

    fun showInfoSheet() {
        _state.update { it.copy(showInfoSheet = true) }
    }

    fun dismissInfoSheet() {
        _state.update { it.copy(showInfoSheet = false) }
    }

    private fun reloadAllTabs() {
        loadDayData(dayOffset)
        loadWeekData(weekOffset)
        loadMonthData(monthOffset)
        loadYearData(yearOffset)
    }

    // region Mock data — TODO: remove once wired to a real data source
    // The helpers below generate deterministic-but-fake step/trend values so
    // the UI can be exercised before the activity pipeline exists. Every caller
    // lives in the loadXxxData() functions and should switch to repository data
    // once it's available.

    private fun randomGoalFraction(random: Random): Float =
        ActivityConstants.MOCK_GOAL_FRACTION_MIN +
                random.nextFloat() *
                (ActivityConstants.MOCK_GOAL_FRACTION_MAX - ActivityConstants.MOCK_GOAL_FRACTION_MIN)

    private fun randomPeriodSteps(random: Random): Int = random.nextInt(
        ActivityConstants.MOCK_PERIOD_STEPS_MIN,
        ActivityConstants.MOCK_PERIOD_STEPS_MAX,
    )

    private fun randomMonthSteps(random: Random): Int = random.nextInt(
        ActivityConstants.MOCK_MONTH_STEPS_MIN,
        ActivityConstants.MOCK_MONTH_STEPS_MAX,
    )

    private fun computeTrend(random: Random, hasComparison: Boolean): Pair<Int?, TrendComparison> {
        if (!hasComparison) return null to TrendComparison.NONE
        val pct = random.nextInt(
            ActivityConstants.MOCK_TREND_PCT_MIN,
            ActivityConstants.MOCK_TREND_PCT_MAX + 1,
        )
        val comparison = when {
            abs(pct) <= ActivityConstants.TREND_FLAT_THRESHOLD -> TrendComparison.FLAT
            pct > 0 -> TrendComparison.UP
            else -> TrendComparison.DOWN
        }
        return pct to comparison
    }

    // endregion

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        val random = Random(day.toEpochDay())
        val isToday = day == LocalDate.now()
        val currentHour = LocalTime.now().hour

        val hourlyBars = (0 until ActivityConstants.HOURS_IN_DAY).map { hour ->
            val isFuture = isToday && hour > currentHour
            val steps =
                if (isFuture) 0 else random.nextInt(0, ActivityConstants.STEP_GRID_MAX.toInt())
            BarChartEntry(
                value = steps.toFloat(),
                xLabel = null,
                tooltipLabel = hour.formatHour(),
            )
        }

        val totalSteps = hourlyBars.sumOf { it.value.toInt() }
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
                hourlyBars = hourlyBars,
                canGoNextDay = offset < 0,
            )
        }
    }

    private fun loadWeekData(offset: Int) {
        val weekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        val random = Random(weekStart.toEpochDay())

        val today = LocalDate.now()
        val days =
            (0 until ActivityConstants.DAYS_IN_WEEK).map { i -> weekStart.plusDays(i.toLong()) }
        val bars = days.map { date ->
            val steps = if (date.isAfter(today)) 0 else randomPeriodSteps(random)
            BarChartEntry(
                value = steps.toFloat(),
                xLabel = date.formatDayOfMonth(),
                tooltipLabel = date.formatShortMonthDay(),
            )
        }
        val summary = buildPeriodSummary(
            bars = bars,
            periodLabel = "${days.first().formatShortMonthDay()} - ${
                days.last().formatShortMonthDay()
            }",
            periodLengthDays = ActivityConstants.DAYS_IN_WEEK,
            random = random,
            canGoNext = offset < 0,
        )

        _state.update { it.copy(week = summary) }
    }

    private fun loadMonthData(offset: Int) {
        val monthStart = LocalDate.now()
            .withDayOfMonth(1)
            .plusMonths(offset.toLong())
        val random = Random(monthStart.toEpochDay())
        val today = LocalDate.now()
        val daysInMonth = monthStart.lengthOfMonth()

        val bars = (0 until daysInMonth).map { i ->
            val date = monthStart.plusDays(i.toLong())
            val steps = if (date.isAfter(today)) 0 else randomPeriodSteps(random)
            BarChartEntry(
                value = steps.toFloat(),
                xLabel = if (date.dayOfMonth in ActivityConstants.MONTH_LABEL_DAYS)
                    date.dayOfMonth.toString() else null,
                tooltipLabel = date.formatShortMonthDay(),
            )
        }
        val summary = buildPeriodSummary(
            bars = bars,
            periodLabel = monthStart.formatFullMonthYear(),
            periodLengthDays = daysInMonth,
            random = random,
            canGoNext = offset < 0,
        )

        _state.update { it.copy(month = summary) }
    }

    private fun loadYearData(offset: Int) {
        val yearStart = LocalDate.now()
            .withDayOfYear(1)
            .plusYears(offset.toLong())
        val random = Random(yearStart.toEpochDay())
        val currentMonthStart = LocalDate.now().withDayOfMonth(1)

        val bars = (0 until ActivityConstants.MONTHS_IN_YEAR).map { i ->
            val monthStart = yearStart.withMonth(Month.JANUARY.value).plusMonths(i.toLong())
            val steps =
                if (monthStart.isAfter(currentMonthStart)) 0 else randomMonthSteps(random)
            BarChartEntry(
                value = steps.toFloat(),
                xLabel = monthStart.formatMonthShort(),
                tooltipLabel = monthStart.formatFullMonthYear(),
            )
        }
        val daysInYear = yearStart.lengthOfYear()
        // Year tab always shows "no comparison" copy per design.
        val summary = buildPeriodSummary(
            bars = bars,
            periodLabel = yearStart.formatYear(),
            periodLengthDays = daysInYear,
            random = random,
            canGoNext = offset < 0,
            hasComparison = false,
        )

        _state.update { it.copy(year = summary) }
    }

    private fun buildPeriodSummary(
        bars: List<BarChartEntry>,
        periodLabel: String,
        periodLengthDays: Int,
        random: Random,
        canGoNext: Boolean,
        hasComparison: Boolean = true,
    ): PeriodSummary {
        val totalSteps = bars.sumOf { it.value.toInt() }
        val avgPerDay = if (periodLengthDays > 0) totalSteps / periodLengthDays else 0
        val (trendPct, trendComparison) = computeTrend(random, hasComparison)
        val current = _state.value
        val distanceKm = avgPerDay * periodLengthDays *
                current.distanceGoalKm / current.stepsGoal.coerceAtLeast(1)
        val calories = (avgPerDay * periodLengthDays *
                current.caloriesGoal.toFloat() / current.stepsGoal.coerceAtLeast(1)).toInt()

        return PeriodSummary(
            periodLabel = periodLabel,
            bars = bars,
            totalSteps = totalSteps,
            avgStepsPerDay = avgPerDay,
            trendPercent = trendPct,
            trendComparison = trendComparison,
            totalDistanceKm = distanceKm,
            totalCalories = calories,
            canGoNext = canGoNext,
        )
    }
}
