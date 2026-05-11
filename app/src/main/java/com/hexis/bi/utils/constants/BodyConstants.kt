package com.hexis.bi.utils.constants

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

internal object BodyConstants {

    /** How many recent scans to load for the trend chart. Covers ~3 months at one scan/day. */
    const val TREND_HISTORY_LIMIT = 90L

    /** Fraction of the (symmetric) half-range at which the outermost grid label sits. */
    const val TOP_GRID_FRACTION = 0.8f

    /** Labelled grid lines as fractions of the half-range — outermost = ±[TOP_GRID_FRACTION]. */
    val GRID_LINE_FRACTIONS = listOf(-TOP_GRID_FRACTION, -0.4f, 0f, 0.4f, TOP_GRID_FRACTION)

    /** Smallest top/bottom grid label the Y-axis will ever show. Keeps a flat history readable. */
    const val MIN_Y_TOP_LABEL = 1.0f

    /** Guard so the chart's value→pixel mapping never divides by zero. */
    const val CHART_MIN_HALF_RANGE = 0.0001f

    /** Top label = maxAbs rounded up to a whole number (≥ [MIN_Y_TOP_LABEL]); half-range = that / [TOP_GRID_FRACTION] for headroom. */
    fun niceYHalfRange(maxAbsValue: Float): Float {
        val topLabel = max(MIN_Y_TOP_LABEL, ceil(abs(maxAbsValue)))
        return topLabel / TOP_GRID_FRACTION
    }

    /** The labelled grid values for a given half-range. */
    fun gridLinesFor(halfRange: Float): List<Float> = GRID_LINE_FRACTIONS.map { it * halfRange }

    /** Y-axis scale used before any data has loaded (a flat ±[MIN_Y_TOP_LABEL] window). */
    val DEFAULT_Y_HALF_RANGE = niceYHalfRange(0f)
    val DEFAULT_GRID_LINES = gridLinesFor(DEFAULT_Y_HALF_RANGE)

    /** Calendar window for the Month range — 30 days back from today, inclusive. */
    const val MONTH_RANGE_DAYS = 30L

    /** Calendar window for the Year range — 12 months back from this month, inclusive. */
    const val YEAR_RANGE_MONTHS = 12L

    /** Number of evenly-spaced labels on the Month X-axis (e.g. 5 labels across 30 days). */
    const val MONTH_LABEL_COUNT = 5

    /** Show every Nth month label on the Year X-axis to avoid crowding (e.g. every 2 months). */
    const val YEAR_LABEL_STEP = 2

    /** When the Year span is this many months or fewer, label every month instead of every Nth. */
    const val YEAR_LABEL_ALL_BELOW_MONTHS = 6
}
