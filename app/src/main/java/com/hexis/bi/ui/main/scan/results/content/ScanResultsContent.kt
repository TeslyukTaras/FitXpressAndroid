package com.hexis.bi.ui.main.scan.results.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.components.AppTabSelector
import com.hexis.bi.ui.main.body.BodyVisualMode
import com.hexis.bi.ui.main.body.CompactSummaryCardHeight
import com.hexis.bi.ui.main.body.CompareContent
import com.hexis.bi.ui.main.body.MyBodyContent
import com.hexis.bi.ui.main.body.VisualContent
import com.hexis.bi.ui.main.scan.results.ResultsState
import com.hexis.bi.ui.main.scan.results.ResultsTab

internal data class ScanResultsActions(
    val onTabSelected: (ResultsTab) -> Unit,
    val onVisualBodyPartSelected: (BodyMeasurementRegion) -> Unit,
    val onCompareBodyPartSelected: (BodyMeasurementRegion) -> Unit,
    val onModeSelected: (BodyVisualMode) -> Unit,
    val onVisualScanSelected: (Long) -> Unit,
    val onCompareLeftScanSelected: (Long) -> Unit,
    val onCompareRightScanSelected: (Long) -> Unit,
    val onModelCardMeasured: (Int) -> Unit,
    val onInfoClick: () -> Unit,
)

@Composable
internal fun ScanResultsContent(
    state: ResultsState,
    actions: ScanResultsActions,
    modifier: Modifier = Modifier,
    onAvatarReady: () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        AppTabSelector(
            tabs = ResultsTab.entries,
            selectedTab = state.selectedTab,
            onTabSelected = actions.onTabSelected,
            tabLabel = { stringResource(it.labelRes) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        )

        val bottomClearance = dimensionResource(R.dimen.spacer_l)

        when (state.selectedTab) {
            ResultsTab.Visual -> VisualContent(
                state = state.visual,
                cardHeightPx = state.modelCardHeightPx,
                isMetric = state.isMetric,
                onBodyPartSelected = actions.onVisualBodyPartSelected,
                onModeSelected = actions.onModeSelected,
                onScanSelected = actions.onVisualScanSelected,
                showScanSelector = false,
                bottomClearance = bottomClearance,
                onAvatarReady = onAvatarReady,
                modifier = Modifier.weight(1f),
            )

            ResultsTab.Compare -> CompareContent(
                state = state.compare,
                cardHeightPx = state.modelCardHeightPx,
                isMetric = state.isMetric,
                onSelectLeftScan = actions.onCompareLeftScanSelected,
                onSelectRightScan = actions.onCompareRightScanSelected,
                onModeSelected = actions.onModeSelected,
                onBodyPartSelected = actions.onCompareBodyPartSelected,
                showScanSelector = false,
                bottomClearance = bottomClearance,
                modifier = Modifier.weight(1f),
            )

            ResultsTab.MyBody -> MyBodyContent(
                visualState = state.visual,
                proportionState = state.bodyProportion,
                cardHeightPx = state.modelCardHeightPx,
                bottomClearance = bottomClearance,
                onAvatarReady = onAvatarReady,
                onInfoClick = actions.onInfoClick,
                modifier = Modifier.weight(1f),
            )
        }

        CompactSummaryCardHeight(
            state = state.visual,
            isMetric = state.isMetric,
            onMeasured = actions.onModelCardMeasured,
        )
    }
}
