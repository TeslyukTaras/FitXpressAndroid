package com.hexis.bi.ui.main.home.activity

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.utils.constants.ActivityConstants
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

data class HourlyStepEntry(
    val hour: Int,
    val steps: Int,
)

data class ActivityState(
    val selectedTab: ActivityTab = ActivityTab.Day,
    val isMetric: Boolean = true,

    // Day tab
    val dateLabel: String = "",
    val stepsGoal: Int = ActivityConstants.DEFAULT_STEP_GOAL,
    val caloriesGoal: Int = ActivityConstants.DEFAULT_CALORIES_GOAL,
    val distanceGoalKm: Float = ActivityConstants.DEFAULT_DISTANCE_GOAL_KM,
    val currentSteps: Int = 0,
    val calories: Int = 0,
    val distanceKm: Float = 0f,
    val hourlySteps: List<HourlyStepEntry> = emptyList(),
    val canGoNextDay: Boolean = false,

    // Info bottom sheet
    val showInfoSheet: Boolean = false,
) {
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
}
