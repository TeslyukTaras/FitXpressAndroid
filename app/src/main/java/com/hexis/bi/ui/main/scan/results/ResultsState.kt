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

data class MeasurementValue(
    val cm: Float,
    val deltaCm: Float,
    val change: MeasurementChange? = null,
)

data class MeasurementRow(
    @StringRes val bodyPartRes: Int,
    /** Key for [MeasurementVisualAnchors] leader table; matches MeasurementMapper `apiKey`. */
    val visualAnchorKey: String,
    val today: MeasurementValue,
    val previous: MeasurementValue?,
)

data class ResultsState(
    val selectedTab: ResultsTab = ResultsTab.Visual,
    val colorAnalysisEnabled: Boolean = false,
    val isMetric: Boolean = true,
    val measurements: List<MeasurementRow> = emptyList(),
    val model3dUrl: String? = null,
    val previousModel3dUrl: String? = null,
    val isPreviewSectionLoading: Boolean = true,
    val todayDate: String = "",
    val previousDate: String? = null,
    val showSkinAreas: Boolean = false,
    /** Firestore document id for this results session (persist preview PNG once). */
    val firestoreScanId: String? = null,
    /** True when [ScanRecord.modelPreviewPngBase64] already exists — skip capture. */
    val modelPreviewAlreadyStored: Boolean = false,
)
