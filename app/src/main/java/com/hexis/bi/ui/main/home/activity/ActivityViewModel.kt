package com.hexis.bi.ui.main.home.activity

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.hexis.bi.data.activity.ActivityRepository
import com.hexis.bi.data.activity.ActivitySummary
import com.hexis.bi.data.terra.TerraRestSourceResolver
import com.hexis.bi.data.user.FirestoreSchema
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.caloriesGoal
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.distanceGoalKm
import com.hexis.bi.utils.formatDayOfMonth
import com.hexis.bi.utils.formatFullMonthDay
import com.hexis.bi.utils.formatFullMonthYear
import com.hexis.bi.utils.formatHour
import com.hexis.bi.utils.formatMonthShort
import com.hexis.bi.utils.formatShortMonthDay
import com.hexis.bi.utils.formatYear
import com.hexis.bi.utils.isMetricUnitSystem
import com.hexis.bi.utils.weekDayAbbreviation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

class ActivityViewModel(
    application: Application,
    private val activityRepository: ActivityRepository,
    private val userRepository: UserRepository,
    private val sourceResolver: TerraRestSourceResolver,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(ActivityState())
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private var dayOffset = 0
    private var weekOffset = 0
    private var monthOffset = 0
    private var yearOffset = 0
    private var weightKg = ProfileConstants.DEFAULT_WEIGHT_KG
    private var heightCm: Float? = null
    private var isFemale: Boolean = false
    private val loadedTabs = mutableSetOf<ActivityTab>()

    init {
        observeDataSource()
        loadDataForTab(_state.value.selectedTab)
        combine(
            userRepository.observeUser(),
            userRepository.observeUserSettings(),
        ) { profile, settings -> profile to settings }
            .onEach { (profile, settings) ->
                heightCm = profile.heightCm?.toFloat()
                isFemale = profile.gender?.lowercase() == "female"
                profile.weightKg?.toFloat()?.let { weightKg = it }

                val stepsGoal = settings.stepsGoal ?: ActivityConstants.DEFAULT_STEP_GOAL
                val showActiveCalories = settings.showActiveCalories ?: true
                val settingsDataSource = settings.activityDataSource
                val (distGoalKm, calGoal) = deriveGoals(stepsGoal)

                _state.update { curr ->
                    curr.copy(
                        isMetric = profile.unitSystem.isMetricUnitSystem(),
                        stepsGoal = stepsGoal,
                        stepsGoalDraft = if (curr.showSettingsDialog) curr.stepsGoalDraft else stepsGoal,
                        showActiveCalories = showActiveCalories,
                        showActiveCaloriesDraft = if (curr.showSettingsDialog) curr.showActiveCaloriesDraft else showActiveCalories,
                        dataSource = settingsDataSource ?: curr.dataSource,
                        distanceGoalKm = distGoalKm,
                        caloriesGoal = calGoal,
                    )
                }
                reloadLoadedTabs()
            }
            .catch { setError(it.message) }
            .launchIn(viewModelScope)
    }

    private fun deriveGoals(stepsGoal: Int): Pair<Float, Int> {
        val height = heightCm
        if (height == null || height <= 0f) {
            return ActivityConstants.DEFAULT_DISTANCE_GOAL_KM to ActivityConstants.DEFAULT_CALORIES_GOAL
        }
        val distGoalKm = distanceGoalKm(stepsGoal, height, isFemale)
        val calGoal = if (distGoalKm > 0f && weightKg > 0f) caloriesGoal(distGoalKm, weightKg)
        else ActivityConstants.DEFAULT_CALORIES_GOAL
        return distGoalKm to calGoal
    }

    fun selectTab(tab: ActivityTab) {
        _state.update { it.copy(selectedTab = tab) }
        if (tab !in loadedTabs) loadDataForTab(tab)
    }

    private fun loadDataForTab(tab: ActivityTab) {
        when (tab) {
            ActivityTab.Day -> loadDayData(dayOffset)
            ActivityTab.Week -> loadWeekData(weekOffset)
            ActivityTab.Month -> loadMonthData(monthOffset)
            ActivityTab.Year -> loadYearData(yearOffset)
        }
    }

    fun retryDayLoad() = loadDayData(dayOffset)
    fun retryWeekLoad() = loadWeekData(weekOffset)
    fun retryMonthLoad() = loadMonthData(monthOffset)
    fun retryYearLoad() = loadYearData(yearOffset)

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
        clearWeekDaySelection()
        loadWeekData(weekOffset)
    }

    fun nextWeek() {
        if (weekOffset < 0) {
            weekOffset++
            clearWeekDaySelection()
            loadWeekData(weekOffset)
        }
    }

    fun selectWeekDay(index: Int) {
        _state.update {
            val resolved = when {
                index !in it.weekDays.indices -> -1
                index == it.selectedWeekDayIndex -> -1 // tap selected day again to clear
                else -> index
            }
            it.copy(selectedWeekDayIndex = resolved)
        }
    }

    fun clearWeekDaySelection() {
        _state.update { it.copy(selectedWeekDayIndex = -1) }
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

    fun showSettingsDialog() {
        _state.update {
            it.copy(
                showSettingsDialog = true,
                stepsGoalDraft = it.stepsGoal,
                showActiveCaloriesDraft = it.showActiveCalories,
            )
        }
    }

    fun dismissSettingsDialog() {
        _state.update { it.copy(showSettingsDialog = false) }
    }

    fun updateStepsGoalDraft(goal: Int) {
        _state.update { it.copy(stepsGoalDraft = goal) }
    }

    fun updateActiveCaloriesDraft(enabled: Boolean) {
        _state.update { it.copy(showActiveCaloriesDraft = enabled) }
    }

    fun saveSettings() {
        val newStepsGoal = _state.value.stepsGoalDraft
        val (distGoalKm, calGoal) = deriveGoals(newStepsGoal)
        _state.update {
            it.copy(
                showSettingsDialog = false,
                stepsGoal = newStepsGoal,
                distanceGoalKm = distGoalKm,
                caloriesGoal = calGoal,
                showActiveCalories = it.showActiveCaloriesDraft,
            )
        }
        viewModelScope.launch {
            userRepository.updateUserSettings(
                mapOf(
                    FirestoreSchema.UserSettingsFields.STEPS_GOAL to newStepsGoal,
                    FirestoreSchema.UserSettingsFields.SHOW_ACTIVE_CALORIES to _state.value.showActiveCaloriesDraft,
                    FirestoreSchema.UserSettingsFields.ACTIVITY_DATA_SOURCE to _state.value.dataSource,
                )
            ).onFailure { setError(it.message) }
        }
        reloadLoadedTabs()
    }

    private fun reloadLoadedTabs() {
        loadedTabs.toList().forEach(::loadDataForTab)
    }

    private fun loadDayData(offset: Int) {
        val day = LocalDate.now().plusDays(offset.toLong())
        _state.update {
            it.copy(
                dayLoadState = ActivityLoadState.Loading,
                dayErrorMessage = null,
                dateLabel = day.formatFullMonthDay(),
                canGoNextDay = offset < 0,
            )
        }
        viewModelScope.launch {
            activityRepository.getSummaryForDate(day).fold(
                onSuccess = { summary ->
                    applyDaySummary(day, summary)
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            dayLoadState = ActivityLoadState.Error,
                            dayErrorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun loadWeekData(offset: Int) {
        val weekStartDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val weekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(weekStartDay))
            .plusWeeks(offset.toLong())
        val weekEnd = weekStart.plusDays(6)
        val previousStart = weekStart.minusWeeks(1)
        val previousEnd = weekEnd.minusWeeks(1)
        _state.update {
            it.copy(
                weekLoadState = ActivityLoadState.Loading,
                weekErrorMessage = null
            )
        }
        viewModelScope.launch {
            activityRepository
                .getSummariesForRange(previousStart, weekEnd)
                .fold(
                    onSuccess = { allRows ->
                        val rows = allRows.filterByDateRange(weekStart, weekEnd)
                        val previousRows = allRows.filterByDateRange(previousStart, previousEnd)
                        loadedTabs.add(ActivityTab.Week)
                        _state.update {
                            it.copy(
                                weekLoadState = ActivityLoadState.Ready,
                                week = buildPeriodSummary(
                                    start = weekStart,
                                    periodLengthDays = ActivityConstants.DAYS_IN_WEEK,
                                    rows = rows,
                                    previousRows = previousRows,
                                    canGoNext = offset < 0,
                                    label = "${weekStart.formatShortMonthDay()} - ${weekEnd.formatShortMonthDay()}",
                                    includeTrend = true,
                                ),
                                weekDays = buildWeekDays(weekStart, rows),
                            )
                        }
                    },
                    onFailure = { failure ->
                        _state.update {
                            it.copy(
                                weekLoadState = ActivityLoadState.Error,
                                weekErrorMessage = failure.message,
                            )
                        }
                    },
                )
        }
    }

    private fun loadMonthData(offset: Int) {
        val monthStart = LocalDate.now()
            .withDayOfMonth(1)
            .plusMonths(offset.toLong())
        val daysInMonth = monthStart.lengthOfMonth()
        val monthEnd = monthStart.plusDays(daysInMonth.toLong() - 1)
        val prevStart = monthStart.minusMonths(1)
        val prevEnd = prevStart.plusDays(prevStart.lengthOfMonth().toLong() - 1)
        _state.update {
            it.copy(
                monthLoadState = ActivityLoadState.Loading,
                monthErrorMessage = null
            )
        }
        viewModelScope.launch {
            activityRepository
                .getSummariesForRange(prevStart, monthEnd)
                .fold(
                    onSuccess = { allRows ->
                        val rows = allRows.filterByDateRange(monthStart, monthEnd)
                        val prevRows = allRows.filterByDateRange(prevStart, prevEnd)
                        loadedTabs.add(ActivityTab.Month)
                        _state.update {
                            it.copy(
                                monthLoadState = ActivityLoadState.Ready,
                                month = buildPeriodSummary(
                                    start = monthStart,
                                    periodLengthDays = daysInMonth,
                                    rows = rows,
                                    previousRows = prevRows,
                                    canGoNext = offset < 0,
                                    label = monthStart.formatFullMonthYear(),
                                    includeTrend = true,
                                ),
                            )
                        }
                    },
                    onFailure = { failure ->
                        _state.update {
                            it.copy(
                                monthLoadState = ActivityLoadState.Error,
                                monthErrorMessage = failure.message,
                            )
                        }
                    },
                )
        }
    }

    private fun loadYearData(offset: Int) {
        val yearStart = LocalDate.now()
            .withDayOfYear(1)
            .plusYears(offset.toLong())
        val yearEnd = yearStart.plusDays(yearStart.lengthOfYear().toLong() - 1)
        _state.update {
            it.copy(
                yearLoadState = ActivityLoadState.Loading,
                yearErrorMessage = null
            )
        }
        viewModelScope.launch {
            activityRepository.getSummariesForRange(yearStart, yearEnd).fold(
                onSuccess = { rows ->
                    loadedTabs.add(ActivityTab.Year)
                    _state.update {
                        it.copy(
                            yearLoadState = ActivityLoadState.Ready,
                            year = buildPeriodSummary(
                                start = yearStart,
                                periodLengthDays = yearStart.lengthOfYear(),
                                rows = rows,
                                previousRows = emptyList(),
                                canGoNext = offset < 0,
                                label = yearStart.formatYear(),
                                includeTrend = false,
                                bucketByMonth = true,
                            ),
                        )
                    }
                },
                onFailure = { err ->
                    _state.update {
                        it.copy(
                            yearLoadState = ActivityLoadState.Error,
                            yearErrorMessage = err.message,
                        )
                    }
                },
            )
        }
    }

    private fun buildPeriodSummary(
        start: LocalDate,
        periodLengthDays: Int,
        rows: List<ActivitySummary>,
        previousRows: List<ActivitySummary>,
        canGoNext: Boolean,
        label: String,
        includeTrend: Boolean,
        bucketByMonth: Boolean = false,
    ): PeriodSummary {
        val byDate = rows.associateBy { it.date }
        val today = LocalDate.now()
        val bars = if (bucketByMonth) {
            val byYearMonth = rows.groupBy { it.date.year to it.date.month }
                .mapValues { (_, list) -> list.sumOf { it.steps } }
            (0 until ActivityConstants.MONTHS_IN_YEAR).map { i ->
                val monthStart = start.withMonth(Month.JANUARY.value).plusMonths(i.toLong())
                val steps = if (monthStart.isAfter(today.withDayOfMonth(1))) 0
                else byYearMonth[monthStart.year to monthStart.month] ?: 0
                BarChartEntry(
                    value = steps.toFloat(),
                    xLabel = monthStart.formatMonthShort(),
                    tooltipLabel = monthStart.formatFullMonthYear(),
                )
            }
        } else {
            (0 until periodLengthDays).map { i ->
                val date = start.plusDays(i.toLong())
                val steps = if (date.isAfter(today)) 0 else (byDate[date]?.steps ?: 0)
                BarChartEntry(
                    value = steps.toFloat(),
                    xLabel = if (periodLengthDays == ActivityConstants.DAYS_IN_WEEK) date.formatDayOfMonth()
                    else if (date.dayOfMonth in ActivityConstants.MONTH_LABEL_DAYS) date.dayOfMonth.toString() else null,
                    tooltipLabel = date.formatShortMonthDay(),
                )
            }
        }

        val totalSteps = bars.sumOf { it.value.toInt() }
        val activeDays = rows.filter { it.steps > 0 }.map { it.date }.distinct().size
        val avgPerDay = if (activeDays > 0) totalSteps / activeDays else 0
        val avgPerDayForTrend = if (activeDays > 0) totalSteps.toFloat() / activeDays else 0f
        // Per-day averages so an in-progress period isn't compared against a full one.
        val previousActiveDays = previousRows.filter { it.steps > 0 }.map { it.date }.distinct().size
        val previousAvgPerDayForTrend =
            if (previousActiveDays > 0) previousRows.sumOf { it.steps }.toFloat() / previousActiveDays
            else 0f
        val trendPct = if (!includeTrend || previousAvgPerDayForTrend <= 0f) null
        else (((avgPerDayForTrend - previousAvgPerDayForTrend) / previousAvgPerDayForTrend) * 100f).toInt()
        val trendComparison = when {
            !includeTrend || trendPct == null -> TrendComparison.NONE
            abs(trendPct) <= ActivityConstants.TREND_FLAT_THRESHOLD -> TrendComparison.FLAT
            trendPct > 0 -> TrendComparison.UP
            else -> TrendComparison.DOWN
        }
        val distanceKm = rows.sumOf { it.distanceKm.toDouble() }.toFloat()
        val calories = rows.sumOf { it.activeCaloriesOrEstimate() }
        val durationSeconds = rows.sumOf { it.activeDurationSeconds }

        return PeriodSummary(
            periodLabel = label,
            bars = bars,
            totalSteps = totalSteps,
            avgStepsPerDay = avgPerDay,
            trendPercent = trendPct,
            trendComparison = trendComparison,
            totalDistanceKm = distanceKm,
            totalCalories = calories,
            totalActiveDurationSeconds = durationSeconds,
            canGoNext = canGoNext,
        )
    }

    private fun buildWeekDays(
        weekStart: LocalDate,
        rows: List<ActivitySummary>,
    ): List<WeekDayData> {
        val byDate = rows.associateBy { it.date }
        val today = LocalDate.now()
        return (0 until ActivityConstants.DAYS_IN_WEEK).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val summary = byDate[date]?.takeUnless { date.isAfter(today) }
            val steps = summary?.steps ?: 0
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            WeekDayData(
                dayLabel = date.weekDayAbbreviation(),
                selectedDateLabel = "$dayName · ${date.formatShortMonthDay()}",
                steps = steps,
                distanceKm = summary?.distanceKm ?: 0f,
                calories = summary?.activeCaloriesOrEstimate() ?: 0,
                durationSeconds = summary?.activeDurationSeconds ?: 0,
                hourlyBars = buildDayBars(date, summary),
                isToday = date == today,
            )
        }
    }

    /**
     * Active calories reported by the provider, or an estimate from distance when they're
     * missing. Some sources (e.g. Health Connect) only return total burned calories with no
     * active/BMR split, which would otherwise leave the metric stuck at 0. The estimate uses
     * the same distance × weight model as the calorie goal, so it stays consistent with the ring.
     */
    private fun ActivitySummary.activeCaloriesOrEstimate(): Int = when {
        activeCalories > 0 -> activeCalories
        weightKg > 0f && distanceKm > 0f -> caloriesGoal(distanceKm, weightKg)
        else -> 0
    }

    private fun applyDaySummary(day: LocalDate, summary: ActivitySummary?) {
        val hourlyBars = buildDayBars(day, summary)
        val totalSteps = summary?.steps ?: 0
        val distanceKm = summary?.distanceKm ?: 0f
        val calories = summary?.activeCaloriesOrEstimate() ?: 0
        val durationSeconds = summary?.activeDurationSeconds ?: 0
        loadedTabs.add(ActivityTab.Day)
        _state.update {
            it.copy(
                dayLoadState = ActivityLoadState.Ready,
                dayErrorMessage = null,
                dateLabel = day.formatFullMonthDay(),
                currentSteps = totalSteps,
                calories = calories,
                distanceKm = distanceKm,
                activeDurationSeconds = durationSeconds,
                hourlyBars = hourlyBars,
                canGoNextDay = dayOffset < 0,
            )
        }
        prefetchWeekIfNeeded()
    }

    private fun buildDayBars(day: LocalDate, summary: ActivitySummary?): List<BarChartEntry> {
        val isToday = day == LocalDate.now()
        val currentHour = LocalTime.now().hour
        val byHour = reconcileHourlyStepsWithTotal(
            hourly = summary?.hourlySteps.orEmpty(),
            totalSteps = summary?.steps ?: 0,
        )
        return (0 until ActivityConstants.HOURS_IN_DAY).map { hour ->
            val isFuture = isToday && hour > currentHour
            val steps = if (isFuture) 0 else (byHour[hour] ?: 0)
            BarChartEntry(
                value = steps.toFloat(),
                xLabel = null,
                tooltipLabel = hour.formatHour(),
            )
        }
    }

    private fun observeDataSource() {
        viewModelScope.launch {
            val provider = sourceResolver.resolveOrderedIdentities()
                .getOrDefault(emptyList())
                .firstOrNull()
                ?.provider
                ?: TerraProviders.HEALTH_CONNECT
            _state.update {
                it.copy(
                    dataSource = provider,
                )
            }
            userRepository.updateUserSettings(
                mapOf(FirestoreSchema.UserSettingsFields.ACTIVITY_DATA_SOURCE to provider)
            ).onFailure { setError(it.message) }
        }
    }

    private fun List<ActivitySummary>.filterByDateRange(
        start: LocalDate,
        end: LocalDate,
    ): List<ActivitySummary> = filter { !it.date.isBefore(start) && !it.date.isAfter(end) }

    private fun prefetchWeekIfNeeded() {
        if (_state.value.selectedTab == ActivityTab.Day && ActivityTab.Week !in loadedTabs) {
            loadWeekData(weekOffset)
        }
    }

    private fun reconcileHourlyStepsWithTotal(
        hourly: Map<Int, Int>,
        totalSteps: Int,
    ): Map<Int, Int> {
        if (hourly.isEmpty() || totalSteps <= 0) return hourly
        val sum = hourly.values.sum()
        if (sum <= totalSteps) return hourly

        val scaled = hourly.mapValues { (_, value) ->
            ((value.toDouble() * totalSteps.toDouble()) / sum.toDouble()).toInt().coerceAtLeast(0)
        }.toMutableMap()
        var diff = totalSteps - scaled.values.sum()
        if (diff != 0) {
            val orderedHours = hourly.entries.sortedByDescending { it.value }.map { it.key }
            var idx = 0
            while (diff != 0 && orderedHours.isNotEmpty()) {
                val hour = orderedHours[idx % orderedHours.size]
                val current = scaled[hour] ?: 0
                if (diff > 0) {
                    scaled[hour] = current + 1
                    diff--
                } else if (current > 0) {
                    scaled[hour] = current - 1
                    diff++
                }
                idx++
            }
        }
        return scaled
    }
}
