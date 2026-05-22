package com.hexis.bi.utils.constants

internal object RecoveryConstants {
    const val ARC_START_ANGLE = 145f
    const val ARC_TOTAL_SWEEP = 250f
    const val GAUGE_GRADIENT_START_STOP = 150f / 360f
    const val GAUGE_GRADIENT_MID_STOP = 270f / 360f
    const val GAUGE_GRADIENT_END_STOP = 30f / 360f
    const val MAX_SCORE = 100f
    val GRID_LINES = listOf(0f, 25f, 50f, 75f, 100f)
    val SUMMARY_GRID_LINES = listOf(0f, 50f, 75f, 100f)
    const val SUMMARY_SCALE_LOW_MAX = 50f
    const val SUMMARY_SCALE_MID_MAX = 75f
    const val SUMMARY_SCALE_BAND_FRACTION = 1f / 3f

    // Recovery — Lifestyle Mode (HEX v4 §5.6): 0.50 HRV + 0.30 RHR + 0.20 Sleep.
    // Activity load is tracked for context only; it does not affect the score in lifestyle mode.
    const val SCORE_HRV_WEIGHT = 0.50f
    const val SCORE_RHR_WEIGHT = 0.30f
    const val SCORE_SLEEP_WEIGHT = 0.20f

    // HRV (RMSSD, ms): below LOW = 0, at/above HIGH = 100 (linear in between).
    const val HRV_LOW_MS = 20f
    const val HRV_HIGH_MS = 70f

    // Resting HR (bpm): inverse — at/below LOW = 100, at/above HIGH = 0. HEX v4 §5.6: 50–85.
    const val RHR_LOW_BPM = 50f
    const val RHR_HIGH_BPM = 85f

    // Threshold buckets for the per-metric labels shown next to the score circle.
    const val STRESS_LOW_HRV_MIN = 50
    const val STRESS_HIGH_HRV_MAX = 30

    const val ACTIVITY_LOAD_LIGHT_MAX = 250
    const val ACTIVITY_LOAD_HEAVY_MIN = 600

    // Score → RecoveryStatus thresholds.
    const val STATUS_READY_MIN = 70
    const val STATUS_RECOVERING_MIN = 40
}
