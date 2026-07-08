package com.hexis.bi.ui.main.scan.results

import androidx.annotation.StringRes
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.BodyProportionState
import com.hexis.bi.ui.main.body.CompareState
import com.hexis.bi.ui.main.body.VisualState

enum class ResultsTab {
    Visual, MyBody, Compare;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            Visual -> R.string.scan_results_tab_visual
            MyBody -> R.string.scan_results_tab_my_body
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
    /** Avatar guide key for visual leaders; can differ from the API value key. */
    val visualAnchorKey: String,
    val today: MeasurementValue,
    val previous: MeasurementValue?,
)

/**
 * The Results screen reuses the My Body Visual/Compare presentation ([VisualState] / [CompareState]
 * rendered by `VisualContent` / `CompareContent`). It is locked to the just-completed (or
 * opened-from-history) scan and its neighbours — there is no scan picker here.
 */
data class ResultsState(
    val selectedTab: ResultsTab = ResultsTab.Visual,
    val isMetric: Boolean = true,
    val isLoading: Boolean = true,
    val visual: VisualState = VisualState(),
    val compare: CompareState = CompareState(),
    val bodyProportion: BodyProportionState = BodyProportionState(),
    val modelCardHeightPx: Int = 0,
    val showPersonalizeResultsHint: Boolean = false,
    val showBodyProportionInfo: Boolean = false,
)

internal val ResultsState.isDisplayable: Boolean
    get() = !isLoading && visual.hasData
