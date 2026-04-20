package com.hexis.bi.utils.constants

import androidx.compose.ui.graphics.Color
import com.hexis.bi.ui.theme.Blue300
import com.hexis.bi.ui.theme.BlueFadedIndicator200
import com.hexis.bi.ui.theme.BlueFadedIndicator300

internal object ActivityConstants {
    const val HOURS_IN_DAY = 24
    const val DEFAULT_STEP_GOAL = 10_000

    // Stride length multipliers (height in cm × factor = stride in cm)
    const val STRIDE_FACTOR_FEMALE = 0.413f
    const val STRIDE_FACTOR_MALE = 0.415f
    const val CM_PER_KM = 100_000f

    // Calorie burn rate (calories per km per kg of body weight)
    const val CALORIES_PER_KM_PER_KG = 0.75f

    // Fallback goals when user profile data is missing
    const val DEFAULT_DISTANCE_GOAL_KM = 8f
    const val DEFAULT_CALORIES_GOAL = 500

    // Circular progress & metric indicators (ordered inner→outer: steps, distance, calories)
    val RING_COLORS: List<Color> = listOf(BlueFadedIndicator200, BlueFadedIndicator300, Blue300)
    const val CIRCLE_FULL_SWEEP = 360f
    const val CIRCLE_START_ANGLE = 0f

    // Day chart grid (hourly steps) — base max grows dynamically in steps of STEP_GRID_STEP
    const val STEP_GRID_MAX = 600f
    const val STEP_GRID_STEP = 200f

    // Week/Month chart grid (daily totals)
    const val PERIOD_STEP_GRID_MAX = 6000f
    const val PERIOD_STEP_GRID_STEP = 2000f

    // Year chart grid (monthly totals)
    const val YEAR_STEP_GRID_MAX = 300_000f
    const val YEAR_STEP_GRID_STEP = 100_000f

    // Period sizing
    const val DAYS_IN_WEEK = 7
    const val MONTHS_IN_YEAR = 12

    // Day-of-month values labelled on the Month tab x-axis
    val MONTH_LABEL_DAYS: Set<Int> = setOf(1, 7, 14, 21, 28)

    // Mock-data ranges (actual values don't derive from step totals)
    const val MOCK_GOAL_FRACTION_MIN = 0.3f
    const val MOCK_GOAL_FRACTION_MAX = 1.2f
    const val MOCK_PERIOD_STEPS_MIN = 1_500
    const val MOCK_PERIOD_STEPS_MAX = 6_000
    const val MOCK_MONTH_STEPS_MIN = 150_000
    const val MOCK_MONTH_STEPS_MAX = 250_000
    const val MOCK_TREND_PCT_MIN = -20
    const val MOCK_TREND_PCT_MAX = 20
    const val TREND_FLAT_THRESHOLD = 2
}
