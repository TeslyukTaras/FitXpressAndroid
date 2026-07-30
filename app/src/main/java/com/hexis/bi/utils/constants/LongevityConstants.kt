package com.hexis.bi.utils.constants

internal object LongevityConstants {

    /** Days of history Home pulls wearable data for; also the Weekly tab span. */
    const val SCORE_WINDOW_DAYS = 7

    // Per-signal normalisation anchors: (value scoring 0) → (value scoring 100), linear between.
    const val HRV_SCORE_AT_ZERO_MS = 20f
    const val HRV_SCORE_AT_HUNDRED_MS = 60f
    const val RHR_SCORE_AT_ZERO_BPM = 75f
    const val RHR_SCORE_AT_HUNDRED_BPM = 50f
    const val ACTIVITY_SCORE_AT_ZERO_STEPS = 0f
    const val ACTIVITY_SCORE_AT_HUNDRED_STEPS = 10_000f
    const val BODY_FAT_SCORE_AT_ZERO_PERCENT = 30f
    const val BODY_FAT_SCORE_AT_HUNDRED_PERCENT = 10f
    // Waist-to-height ratio: ≥0.6 (high visceral-fat risk) scores 0, ≤0.4 (lean) scores 100.
    const val WAIST_HEIGHT_SCORE_AT_ZERO = 0.6f
    const val WAIST_HEIGHT_SCORE_AT_HUNDRED = 0.4f
    const val VO2_SCORE_AT_ZERO = 30f
    const val VO2_SCORE_AT_HUNDRED = 55f

    /** Min absolute change in score (or ratio %) for a trend to count as improving/decreasing. */
    const val TREND_FLAT_THRESHOLD = 2f
}
