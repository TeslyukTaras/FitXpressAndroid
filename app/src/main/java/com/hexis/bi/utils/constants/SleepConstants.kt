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

    // Sleep Score (HEX v1): SS = 0.70 × duration_score + 0.30 × efficiency_score
    const val SCORE_DURATION_WEIGHT = 0.70f
    const val SCORE_EFFICIENCY_WEIGHT = 0.30f
    const val SCORE_DURATION_LOW_HOURS = 5.5f
    const val SCORE_DURATION_HIGH_HOURS = 8.5f
    const val SCORE_EFFICIENCY_LOW_PCT = 70f
    const val SCORE_EFFICIENCY_HIGH_PCT = 90f

    // Score → SleepQuality thresholds
    const val QUALITY_GOOD_MIN = 70
    const val QUALITY_FAIR_MIN = 40

    const val DEFAULT_SLEEP_GOAL_HOURS = 8
}
