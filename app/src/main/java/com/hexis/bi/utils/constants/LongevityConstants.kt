package com.hexis.bi.utils.constants

internal object LongevityConstants {
    const val MAX_SCORE = 100f

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
}
