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

    // Steps timeline chart
    val STEP_GRID_LINES = listOf(0f, 200f, 400f, 600f)
    const val STEP_GRID_MAX = 600f

    // Mock data: actual distance/calories are randomized as a fraction of their goals
    const val MOCK_GOAL_FRACTION_MIN = 0.3f
    const val MOCK_GOAL_FRACTION_MAX = 1.2f
}
