package com.hexis.bi.utils.constants

import com.hexis.bi.utils.constants.BodyConstants.CHART_FILL_END_ALPHA
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

    const val FOUR_WEEK_SPAN_DAYS = 28L

    const val SIX_MONTH_SPAN = 6L

    const val ONE_YEAR_SPAN_MONTHS = 12L

    const val SIX_MONTH_LABEL_STEP = 1L

    const val ONE_YEAR_LABEL_STEP = 2L

    const val SCAN_CADENCE_DAYS = 7L

    const val LEGEND_INACTIVE_ALPHA = 0.4f

    const val CHART_MONOTONE_TANGENT_LIMIT = 3f
    const val CHART_FILL_OPACITY = 0.26f

    /** Drop-shadow fill alpha at the line's extreme (above/below zero); fades to [CHART_FILL_END_ALPHA] at the zero axis. */
    const val CHART_FILL_START_ALPHA = 0.83f
    const val CHART_FILL_END_ALPHA = 0.0001f


}
