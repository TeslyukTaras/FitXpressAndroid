package com.hexis.bi.ui.main.body

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.utils.constants.BodyConstants

enum class BodyTab {
    Stats,
    Visual,
    MyBody,
    Compare;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Stats -> R.string.body_tab_stats
            Visual -> R.string.body_tab_visual
            MyBody -> R.string.body_tab_my_body
            Compare -> R.string.body_tab_compare
        }
}

enum class BodyMassUnit {
    Percent,
    Mass;
}

enum class BodyTimeRange {
    Week,
    Month,
    Year;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Week -> R.string.body_range_week
            Month -> R.string.body_range_month
            Year -> R.string.body_range_year
        }

    @get:StringRes
    val periodLabelRes: Int
        get() = when (this) {
            Week -> R.string.body_physique_drift_period_week
            Month -> R.string.body_physique_drift_period_month
            Year -> R.string.body_physique_drift_period_year
        }
}

enum class BodyLoadState { Loading, Ready, Error }

data class VisualScanOption(
    val timestamp: Long,
)

enum class BodyVisualMode {
    Base,
    Color,
}

sealed interface BodyVisualColorModel {
    data object Idle : BodyVisualColorModel
    data object Loading : BodyVisualColorModel
    data class Ready(val coloredModelUrl: String) : BodyVisualColorModel
    data object Unavailable : BodyVisualColorModel
    data object Error : BodyVisualColorModel
}

data class VisualState(
    val selectedBodyPart: BodyMeasurementRegion = BodyMeasurementRegion.FullBody,
    val mode: BodyVisualMode = BodyVisualMode.Base,
    val colorModel: BodyVisualColorModel = BodyVisualColorModel.Idle,
    val hasData: Boolean = false,
    val isLatestScanSelected: Boolean = true,
    val scanOptions: List<VisualScanOption> = emptyList(),
    val latestScanTimestamp: Long? = null,
    val previousScanTimestamp: Long? = null,
    val beforePreviousScanTimestamp: Long? = null,
    val latestModel3dUrl: String? = null,
    val previousModel3dUrl: String? = null,
    val latestMeasurements: Map<String, Float> = emptyMap(),
    val previousMeasurements: Map<String, Float> = emptyMap(),
    val beforePreviousMeasurements: Map<String, Float> = emptyMap(),
    /** Regions the user chose to show in measurement tables (Scan Preference). */
    val visibleRegions: Set<BodyMeasurementRegion> = BodyMeasurementRegion.measurableRegions.toSet(),
)

data class CompareState(
    val hasData: Boolean = false,
    val mode: BodyVisualMode = BodyVisualMode.Base,
    val selectedBodyPart: BodyMeasurementRegion = BodyMeasurementRegion.FullBody,
    val scanOptions: List<VisualScanOption> = emptyList(),
    val leftScanTimestamp: Long? = null,
    val rightScanTimestamp: Long? = null,
    val leftModel3dUrl: String? = null,
    val rightModel3dUrl: String? = null,
    val leftMeasurements: Map<String, Float> = emptyMap(),
    val leftPreviousMeasurements: Map<String, Float> = emptyMap(),
    val rightMeasurements: Map<String, Float> = emptyMap(),
    val rightPreviousMeasurements: Map<String, Float> = emptyMap(),
    val leftColorModel: BodyVisualColorModel = BodyVisualColorModel.Idle,
    val rightColorModel: BodyVisualColorModel = BodyVisualColorModel.Idle,
    /** Regions the user chose to show in measurement tables (Scan Preference). */
    val visibleRegions: Set<BodyMeasurementRegion> = BodyMeasurementRegion.measurableRegions.toSet(),
)

data class BodyProportionState(
    val hasData: Boolean = false,
    val isFemaleProfile: Boolean = false,
    val groups: List<BodyProportionGroup> = emptyList(),
)

data class BodyProportionGroup(
    @StringRes val titleRes: Int,
    val markers: List<BodyProportionMarker>,
)

data class BodyProportionMarker(
    @StringRes val labelRes: Int,
    val value: Float?,
    @StringRes val statusRes: Int,
    val progress: Float,
)

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

data class BodyTrendPoint(
    val timestamp: Long,
    val deltaFat: Float,
    val deltaMuscle: Float,
    val absoluteFat: Float,
    val absoluteMuscle: Float,
    val isInterpolated: Boolean = false,
    val phase: BodyTrendPhase = BodyTrendPhase.ConfirmedScan,
)

enum class BodyTrendPhase {
    ConfirmedScan,
    PredictedDrift,
    FutureEstimate,
}

data class BodyChartAxisLabel(
    val timestamp: Long,
    val text: String,
)

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
    val timeRange: BodyTimeRange = BodyTimeRange.Week,
    val composition: BodyComposition = BodyComposition.empty(),
    val periodPhysiqueDrift: Float? = null,
    val chart: BodyChartData = BodyChartData(0L, 0L),
    val showBisInfo: Boolean = false,
    val showBodyProportionInfo: Boolean = false,
    val visual: VisualState = VisualState(),
    val compare: CompareState = CompareState(),
    val bodyProportion: BodyProportionState = BodyProportionState(),
    val modelCardHeightPx: Int = 0,
)
