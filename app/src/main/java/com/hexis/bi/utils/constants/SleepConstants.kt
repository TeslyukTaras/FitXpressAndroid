package com.hexis.bi.utils.constants

internal object SleepConstants {
    const val MINUTES_PER_HOUR = 60
    const val SLEEP_STAGE_COUNT = 4
    const val SLEEP_GOAL_MIN_HOURS = 5
    const val SLEEP_GOAL_MAX_HOURS = 12
    const val ARC_START_ANGLE = 150f
    const val ARC_TOTAL_SWEEP = 240f
    const val ARC_CAP_ENDS_PER_GAP = 2f   // each gap between segments has two cap ends
    const val BAR_CHART_STRIPE_FLATTEN = 3f  // diagonal slope of background stripes (higher = flatter)
    const val BAR_CHART_STRIPE_BG_ALPHA = 0.2f  // opacity of the stripe background tint
}
