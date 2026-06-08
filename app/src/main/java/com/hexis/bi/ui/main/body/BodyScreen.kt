package com.hexis.bi.ui.main.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.dark.DarkTabSelector
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.body.components.BisInfoBottomSheet
import com.hexis.bi.ui.theme.dark.DarkTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    onHistoryClick: () -> Unit,
    onPhysiqueBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    DarkTheme {
        BaseScreen(
            modifier = modifier
                .fillMaxSize()
                .then(if (state.showBisInfo) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop)) else Modifier)
                .darkScreenBackground(),
            containerColor = Color.Transparent,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.body_screen_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.icon_normalized))
                            .align(Alignment.Top),
                        onClick = onHistoryClick,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Transparent,
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.cd_body_history),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                DarkTabSelector(
                    tabs = BodyTab.entries,
                    selectedTab = state.selectedTab,
                    onTabSelected = viewModel::selectTab,
                    tabLabel = { stringResource(it.labelRes) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                )

                when (state.selectedTab) {
                    BodyTab.Stats, BodyTab.Posture ->
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                    BodyTab.Visual, BodyTab.Compare -> Unit
                }

                when (state.selectedTab) {
                    BodyTab.Stats -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                    ) {
                        StatsContent(
                            state = state,
                            onMassUnitChange = viewModel::selectMassUnit,
                            onTimeRangeChange = viewModel::selectTimeRange,
                            onPhysiqueBalanceClick = onPhysiqueBalanceClick,
                            onRetry = viewModel::retry,
                        )
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                    }

                    BodyTab.Visual -> VisualContent(
                        state = state.visual,
                        cardHeightPx = state.modelCardHeightPx,
                        isMetric = state.isMetric,
                        onBodyPartSelected = viewModel::selectBodyPart,
                        onModeSelected = viewModel::selectMode,
                        onScanSelected = viewModel::selectVisualScan,
                        modifier = Modifier.weight(1f),
                    )

                    BodyTab.Compare -> CompareContent(
                        state = state.compare,
                        cardHeightPx = state.modelCardHeightPx,
                        isMetric = state.isMetric,
                        onSelectLeftScan = viewModel::selectCompareLeftScan,
                        onSelectRightScan = viewModel::selectCompareRightScan,
                        onModeSelected = viewModel::selectMode,
                        onBodyPartSelected = viewModel::selectCompareBodyPart,
                        modifier = Modifier.weight(1f),
                    )

                    BodyTab.Posture -> {
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                        Text(
                            text = stringResource(R.string.body_tab_coming_soon),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                        )
                    }
                }

                CompactSummaryCardHeight(
                    state = state.visual,
                    isMetric = state.isMetric,
                    onMeasured = viewModel::setModelCardHeight,
                )
            }
        }

        if (state.showBisInfo) BisInfoBottomSheet(onDismiss = viewModel::dismissBisInfo)
    }
}
