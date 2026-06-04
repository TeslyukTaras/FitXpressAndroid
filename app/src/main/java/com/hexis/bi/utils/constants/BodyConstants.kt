package com.hexis.bi.utils.constants

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

internal object BodyConstants {

    /** Recent scans retained for trend history. */
    const val TREND_HISTORY_LIMIT = 90L

    const val VISUAL_SCAN_OPTION_LIMIT = 10

    /** Outer grid labels sit on the chart bounds. */
    const val TOP_GRID_FRACTION = 1f

    /** Grid positions within each half-range. */
    val GRID_LINE_FRACTIONS = listOf(-TOP_GRID_FRACTION, -0.5f, 0f, 0.5f, TOP_GRID_FRACTION)

    /** Smallest outer Y-axis label. */
    const val MIN_Y_TOP_LABEL = 1.0f

    /** Minimum nonzero Y span. */
    const val CHART_MIN_HALF_RANGE = 0.0001f

    /** Expands a half-range to fit the largest delta. */
    fun niceYHalfRange(maxAbsValue: Float): Float {
        val topLabel = max(MIN_Y_TOP_LABEL, ceil(abs(maxAbsValue)))
        return topLabel / TOP_GRID_FRACTION
    }

    /** Grid labels for a half-range. */
    fun gridLinesFor(halfRange: Float): List<Float> = GRID_LINE_FRACTIONS.map { it * halfRange }

    /** Empty-data Y scale. */
    val DEFAULT_Y_HALF_RANGE = niceYHalfRange(0f)
    val DEFAULT_GRID_LINES = gridLinesFor(DEFAULT_Y_HALF_RANGE)

    /** Three days on either side of today for the Week viewport. */
    const val WEEK_HALF_DAYS = 3L

    /** Fifteen days on either side of today for the Month viewport. */
    const val MONTH_HALF_DAYS = 15L

    /** Six calendar months on either side of today for the Year viewport. */
    const val YEAR_HALF_MONTHS = 6L

    /** Recommended days between scans; drives the "Next Scan" countdown. */
    const val SCAN_CADENCE_DAYS = 7L

    /**
     * Leading portion of the forecast styled as short-term predicted drift (dashed); the remainder
     * is the future estimate (dotted). The forecast tail is drawn with a handful of nodes (not one
     * per day), split evenly between the two phases, with drift realized on an easeIn curve so the
     * fan starts gently near the origin and accelerates outward to its max at the final point.
     */
    const val PREDICTED_DRIFT_FRACTION = 0.4f

    const val CHART_MONOTONE_TANGENT_LIMIT = 3f
    const val CHART_FILL_OPACITY = 0.26f
    /** Drop-shadow fill alpha at the line's extreme (above/below zero); fades to [CHART_FILL_END_ALPHA] at the zero axis. */
    const val CHART_FILL_START_ALPHA = 0.83f
    const val CHART_FILL_END_ALPHA = 0.0001f

    const val PHYSIQUE_SCORE_MIN = 1f
    const val PHYSIQUE_SCORE_MAX = 10f
    const val PHYSIQUE_WEIGHT_BODY_FAT = 0.5f
    const val PHYSIQUE_WEIGHT_LEAN_MASS = 0.25f
    const val PHYSIQUE_WEIGHT_WAIST_SHAPE = 0.15f
    const val PHYSIQUE_WEIGHT_PROPORTION = 0.1f

    /** Month X-axis label count. */
    const val MONTH_LABEL_COUNT = 5

    /** Year X-axis label cadence. */
    const val YEAR_LABEL_STEP = 2

    /** Label every month at or below this year-span threshold. */
    const val YEAR_LABEL_ALL_BELOW_MONTHS = 6
}
