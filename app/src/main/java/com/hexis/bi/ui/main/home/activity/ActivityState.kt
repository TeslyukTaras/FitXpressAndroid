package com.hexis.bi.ui.main.home.activity

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.TerraProviders
import com.hexis.bi.utils.kmToMiles

enum class ActivityTab(@StringRes val labelRes: Int) {
    Day(R.string.activity_tab_day),
    Week(R.string.activity_tab_week),
    Month(R.string.activity_tab_month),
    Year(R.string.activity_tab_year),
}

data class ActivityMetric(
    @StringRes val labelRes: Int,
    val value: String,
    val unit: String,
)

data class BarChartEntry(
    val value: Float,
    val xLabel: String? = null,
    val tooltipLabel: String,
)

enum class TrendComparison { UP, DOWN, FLAT, NONE }
enum class ActivityLoadState { Loading, Ready, Error }

data class WeekDayData(
    val dayLabel: String = "",
    val selectedDateLabel: String = "",
    val steps: Int = 0,
    val distanceKm: Float = 0f,
    val calories: Int = 0,
    val durationSeconds: Int = 0,
    val hourlyBars: List<BarChartEntry> = emptyList(),
    val isToday: Boolean = false,
)

data class PeriodSummary(
    val periodLabel: String = "",
    val bars: List<BarChartEntry> = emptyList(),
    val totalSteps: Int = 0,
    val avgStepsPerDay: Int = 0,
    val trendPercent: Int? = null,
    val trendComparison: TrendComparison = TrendComparison.NONE,
    val totalDistanceKm: Float = 0f,
    val totalCalories: Int = 0,
    val totalActiveDurationSeconds: Int = 0,
    val canGoNext: Boolean = false,
)

data class ActivityState(
    val selectedTab: ActivityTab = ActivityTab.Day,
    val isMetric: Boolean = true,

    // Day tab
    val dayLoadState: ActivityLoadState = ActivityLoadState.Loading,
    val dayErrorMessage: String? = null,
    val dateLabel: String = "",
    val stepsGoal: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val caloriesGoal: Int = ActivityConstants.DEFAULT_CALORIES_GOAL,
    val distanceGoalKm: Float = ActivityConstants.DEFAULT_DISTANCE_GOAL_KM,
    val currentSteps: Int = 0,
    val calories: Int = 0,
    val distanceKm: Float = 0f,
    val activeDurationSeconds: Int = 0,
    val hourlyBars: List<BarChartEntry> = emptyList(),
    val canGoNextDay: Boolean = false,

    // Period tabs
    val weekLoadState: ActivityLoadState = ActivityLoadState.Loading,
    val weekErrorMessage: String? = null,
    val week: PeriodSummary = PeriodSummary(),
    val weekDays: List<WeekDayData> = emptyList(),
    val selectedWeekDayIndex: Int = -1,
    val monthLoadState: ActivityLoadState = ActivityLoadState.Loading,
    val monthErrorMessage: String? = null,
    val month: PeriodSummary = PeriodSummary(),
    val yearLoadState: ActivityLoadState = ActivityLoadState.Loading,
    val yearErrorMessage: String? = null,
    val year: PeriodSummary = PeriodSummary(),

    // Info bottom sheet
    val showInfoSheet: Boolean = false,

    // Settings dialog
    val showSettingsDialog: Boolean = false,
    val stepsGoalDraft: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val showActiveCalories: Boolean = true,
    val showActiveCaloriesDraft: Boolean = true,
    val dataSource: String = TerraProviders.HEALTH_CONNECT,
) {
    val dataSourceName: String
        get() = dataSource.toProviderDisplayName()

    val progressPercent: Int
        get() = if (stepsGoal > 0)
            ((currentSteps.toFloat() / stepsGoal) * 100).toInt().coerceIn(0, 100)
        else 0

    val distanceDisplay: Float
        get() = if (isMetric) distanceKm else distanceKm.kmToMiles()

    @get:StringRes
    val distanceUnitRes: Int
        get() = if (isMetric) R.string.activity_unit_km else R.string.activity_unit_mi

    val distanceGoal: Float
        get() = if (isMetric) distanceGoalKm else distanceGoalKm.kmToMiles()

    val selectedWeekDay: WeekDayData?
        get() = weekDays.getOrNull(selectedWeekDayIndex)
}

internal fun String.toProviderDisplayName(): String =
    when {
        equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) -> "Google Health"
        equals("GoogleHealth", ignoreCase = true) -> "Google Health"
        equals("GOOGLE", ignoreCase = true) -> "Google"
        equals("AppleHealth", ignoreCase = true) -> "Apple Health"
        equals("APPLE", ignoreCase = true) ||
                equals("APPLE_HEALTH", ignoreCase = true) -> "Apple Health"
        else -> split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
            .ifBlank { this }
    }
