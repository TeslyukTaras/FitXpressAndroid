package com.hexis.bi.intelligence.prediction

object PredictionConstants {

    const val SCHEMA_VERSION = 1
    const val ALGORITHM_VERSION = 1

    const val HORIZON_DAYS = 7
    const val WINDOW_BUCKETS = 4
    const val MIN_BUCKETS = 2
    const val MAX_WEEKLY_DELTA = 1.5

    const val DEFAULT_GAIN = 1.0
    const val MIN_GAIN = 0.3
    const val MAX_GAIN = 1.5
    const val GAIN_STEP = 0.08

    const val MIN_RATIO = 0.0
    const val MAX_RATIO = 2.0

    const val MIN_ELAPSED_DAYS = 3
    const val MAX_ELAPSED_DAYS = 14
    const val IMPLAUSIBLE_DELTA = 3.0 * MAX_WEEKLY_DELTA

    const val APPLY_GAINS = false
}
