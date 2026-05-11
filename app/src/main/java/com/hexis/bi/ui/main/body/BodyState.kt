package com.hexis.bi.ui.main.body

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.utils.constants.BodyConstants

enum class BodyTab {
    Stats,
    Visual,
    Posture,
    Compare;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Stats -> R.string.body_tab_stats
            Visual -> R.string.body_tab_visual
            Posture -> R.string.body_tab_posture
            Compare -> R.string.body_tab_compare
        }
}

enum class BodyMassUnit {
    /** Body fat / muscle mass shown as percentage of total weight. */
    Percent,

    /** Body fat / muscle mass shown as estimated mass (kg or lb). */
    Mass;
}

enum class BodyTimeRange {
    Month,
    Year;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Month -> R.string.body_range_month
            Year -> R.string.body_range_year
        }
}

enum class BodyLoadState { Loading, Ready, Error }

/** Snapshot of body composition values for one scan, with deltas vs the previous scan. */
data class BodyComposition(
    val timestamp: Long,
    val weightKg: Float?,
    val bmi: Float?,
    val fatPercentage: Float?,
    val muscleMassPercentage: Float?,
    val fatMassKg: Float?,
    val muscleMassKg: Float?,
    val bisScore: Float?,
    val deltaWeightKg: Float?,
    val deltaBmi: Float?,
    val deltaFatPercentage: Float?,
    val deltaMuscleMassPercentage: Float?,
    val deltaFatMassKg: Float?,
    val deltaMuscleMassKg: Float?,
    val deltaBisScore: Float?,
) {
    companion object {
        /** Placeholder shown to users with no scans yet — every value tile renders as "—". */
        fun empty() = BodyComposition(
            timestamp = 0L,
            weightKg = null, bmi = null,
            fatPercentage = null, muscleMassPercentage = null,
            fatMassKg = null, muscleMassKg = null, bisScore = null,
            deltaWeightKg = null, deltaBmi = null,
            deltaFatPercentage = null, deltaMuscleMassPercentage = null,
            deltaFatMassKg = null, deltaMuscleMassKg = null, deltaBisScore = null,
        )
    }
}

/**
 * A point on the trend curve, positioned by [timestamp]. Deltas are change vs the first point in
 * view, in the chart's current display unit (pp or kg/lb); absolutes are kept for re-baselining.
 * [isInterpolated] points are gap-fill between two real scans — drawn, but not pointer-snap targets.
 */
data class BodyTrendPoint(
    val timestamp: Long,
    val deltaFat: Float,
    val deltaMuscle: Float,
    val absoluteFat: Float,
    val absoluteMuscle: Float,
    val isInterpolated: Boolean = false,
)

/** A label rendered along the chart's X-axis at a fixed time. */
data class BodyChartAxisLabel(
    val timestamp: Long,
    val text: String,
)

/** A single range to draw. Empty [points] still renders the axes. Y-axis is symmetric [-yAxisBound, +yAxisBound] so 0 is centred. */
data class BodyChartData(
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val points: List<BodyTrendPoint> = emptyList(),
    val axisLabels: List<BodyChartAxisLabel> = emptyList(),
    val rangeLabel: String = "",
    val yAxisBound: Float = BodyConstants.DEFAULT_Y_HALF_RANGE,
    val gridLines: List<Float> = BodyConstants.DEFAULT_GRID_LINES,
)

data class BodyState(
    val selectedTab: BodyTab = BodyTab.Stats,
    val loadState: BodyLoadState = BodyLoadState.Loading,
    val isMetric: Boolean = true,
    val massUnit: BodyMassUnit = BodyMassUnit.Percent,
    val timeRange: BodyTimeRange = BodyTimeRange.Month,
    val composition: BodyComposition = BodyComposition.empty(),
    val chart: BodyChartData = BodyChartData(0L, 0L),
    val showBisInfo: Boolean = false,
)
