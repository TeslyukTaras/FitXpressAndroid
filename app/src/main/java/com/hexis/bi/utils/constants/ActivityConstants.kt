package com.hexis.bi.utils.constants

internal object ActivityConstants {
    const val HOURS_IN_DAY = 24
    const val DEFAULT_STEP_GOAL = 10_000

    // Settings dialog — steps-goal slider bounds (in steps)
    const val STEPS_GOAL_MIN = 2_000
    const val STEPS_GOAL_MAX = 25_000
    const val STEPS_GOAL_SLIDER_STEP = 500

    // Stride length multipliers (height in cm × factor = stride in cm)
    const val STRIDE_FACTOR_FEMALE = 0.413f
    const val STRIDE_FACTOR_MALE = 0.415f
    const val CM_PER_KM = 100_000f

    // Calorie burn rate (calories per km per kg of body weight)
    const val CALORIES_PER_KM_PER_KG = 0.75f

    // Fallback goals when user profile data is missing
    const val DEFAULT_DISTANCE_GOAL_KM = 8f
    const val DEFAULT_CALORIES_GOAL = 500

    // Circular progress & metric indicators are coloured from the theme; see
    // NocturnePulseTheme.extendedColors (activityStepsProgress / activityDistanceProgress /
    // accentBlue, ordered inner→outer: steps, distance, calories) and activityProgressTrack.
    const val CIRCLE_FULL_SWEEP = 360f
    const val CIRCLE_START_ANGLE = 0f
    const val GAUGE_START_ANGLE = 180f
    const val GAUGE_FULL_SWEEP = 180f

    // Day chart grid (hourly steps) — base max grows dynamically in steps of STEP_GRID_STEP
    const val STEP_GRID_MAX = 600f
    const val STEP_GRID_STEP = 200f
    const val Y_AXIS_HEADROOM_FRACTION = 0.08f
    const val Y_AXIS_MIN_GRID_STEP = 10f

    // Week/Month chart grid (daily totals)
    const val PERIOD_STEP_GRID_MAX = 6000f
    const val PERIOD_STEP_GRID_STEP = 2000f

    // Year chart grid (monthly totals)
    const val YEAR_STEP_GRID_MAX = 6_000f
    const val YEAR_STEP_GRID_STEP = 2_000f

    // Period sizing
    const val DAYS_IN_WEEK = 7
    const val MONTHS_IN_YEAR = 12

    // Day-of-month values labeled on the Month tab x-axis
    val MONTH_LABEL_DAYS: Set<Int> = setOf(1, 7, 14, 21, 28)

    // Mock-data ranges
    const val TREND_FLAT_THRESHOLD = 2
}
