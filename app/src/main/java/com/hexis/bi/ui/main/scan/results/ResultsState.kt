package com.hexis.bi.ui.main.scan.results

import androidx.annotation.StringRes
import com.hexis.bi.R

enum class ResultsTab {
    Visual, Posture, Compare;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Visual -> R.string.scan_results_tab_visual
            Posture -> R.string.scan_results_tab_posture
            Compare -> R.string.scan_results_tab_compare
        }
}

enum class MeasurementChange {
    Positive, Negative
}

/** A single measurement cell: the value and the delta vs the prior reading. */
data class MeasurementValue(
    val cm: Float,
    val deltaCm: Float,
    val change: MeasurementChange? = null,
)

data class MeasurementRow(
    @StringRes val bodyPartRes: Int,
    val today: MeasurementValue,
    val previous: MeasurementValue,
)

data class ResultsState(
    val selectedTab: ResultsTab = ResultsTab.Visual,
    val colorAnalysisEnabled: Boolean = false,
    val isMetric: Boolean = true,
    val measurements: List<MeasurementRow> = defaultMeasurements,
)

private val defaultMeasurements = listOf(
    MeasurementRow(R.string.scan_measurement_neck, MeasurementValue(118.0f, 1.2f, MeasurementChange.Positive), MeasurementValue(118.0f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_shoulders, MeasurementValue(118.0f, 1.2f, MeasurementChange.Negative), MeasurementValue(118.0f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_chest, MeasurementValue(102.0f, 1.2f, MeasurementChange.Positive), MeasurementValue(104.0f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_forearms, MeasurementValue(89.0f, 1.2f, MeasurementChange.Negative), MeasurementValue(89.0f, -1.1f)),
    MeasurementRow(R.string.scan_measurement_biceps, MeasurementValue(89.0f, 1.2f, MeasurementChange.Positive), MeasurementValue(89.0f, -1.1f)),
    MeasurementRow(R.string.scan_measurement_upper_waist, MeasurementValue(96.5f, 1.2f, MeasurementChange.Positive), MeasurementValue(96.5f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_mid_waist, MeasurementValue(96.5f, 1.2f, MeasurementChange.Negative), MeasurementValue(96.5f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_lower_waist, MeasurementValue(96.5f, 1.2f, MeasurementChange.Positive), MeasurementValue(96.5f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_thigh, MeasurementValue(52.2f, -1.1f, MeasurementChange.Negative), MeasurementValue(52.4f, 1.2f)),
    MeasurementRow(R.string.scan_measurement_calf, MeasurementValue(38.5f, 1.2f, MeasurementChange.Positive), MeasurementValue(38.6f, 1.3f)),
)
