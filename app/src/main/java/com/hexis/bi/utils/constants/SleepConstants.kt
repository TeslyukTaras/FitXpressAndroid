package com.hexis.bi.utils.constants

internal object SleepConstants {
    const val MINUTES_PER_HOUR = 60
    const val SLEEP_GOAL_MIN_HOURS = 5
    const val SLEEP_GOAL_MAX_HOURS = 12

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

    // Sleep timeline x-axis: number of evenly spaced hour labels.
    const val TIMELINE_LABEL_COUNT = 5
}
