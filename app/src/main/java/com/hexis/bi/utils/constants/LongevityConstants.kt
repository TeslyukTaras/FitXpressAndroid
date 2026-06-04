package com.hexis.bi.utils.constants

internal object LongevityConstants {
    const val MAX_SCORE = 100f

    /** Days of history the current score scans (newest day with data wins); also the Weekly tab span. */
    const val SCORE_WINDOW_DAYS = 7

    /** Y-axis grid values for the longevity trend chart (bottom to top). */
    val GRID_LINES = listOf(0f, 25f, 70f, 100f)

    // Non-linear y-axis: each band between grid values (0–25, 25–70, 70–100) gets equal visual
    // height, so the unevenly spaced scores read as evenly spaced rows.
    const val BAND_FRACTION = 1f / 3f
    const val BAND_LOW_MAX = 25f
    const val BAND_MID_MAX = 70f

    // Smooth line fill gradient
    const val CHART_FILL_START_STOP = 0f
    const val CHART_FILL_END_STOP = 1f
    const val CHART_FILL_START_ALPHA = 0.35f
    const val CHART_FILL_END_ALPHA = 0.0f

    /** Limits the monotone-cubic tangents so the smoothed line never overshoots. */
    const val CHART_MONOTONE_TANGENT_LIMIT = 3f

    // Healthy-aging score weights (sum to 1.0). Missing signals drop out and are renormalised.
    // Body fat and waist-to-height together form the spec's 15% "Body Fat / Waist" bucket, split
    // evenly so either can stand in for the bucket when the other is unavailable.
    const val WEIGHT_HRV = 0.25f
    const val WEIGHT_SLEEP = 0.20f
    const val WEIGHT_ACTIVITY = 0.15f
    const val WEIGHT_RHR = 0.15f
    const val WEIGHT_BODY_FAT = 0.075f
    const val WEIGHT_WAIST = 0.075f
    const val WEIGHT_VO2 = 0.10f

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
