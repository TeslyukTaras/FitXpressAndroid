package com.hexis.bi.ui.main.body

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppTabSelector
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.components.MedicalDisclaimerFooter
import com.hexis.bi.ui.main.body.components.BisInfoBottomSheet
import com.hexis.bi.ui.main.body.components.BodyMetricTile
import com.hexis.bi.ui.main.body.components.BodyTrendChart
import com.hexis.bi.ui.main.body.components.PhysiqueMetricCard
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.utils.constants.ChangeTolerances
import org.koin.androidx.compose.koinViewModel

@Composable
fun PhysiqueBalanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BodyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .then(if (state.showBisInfo) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop)) else Modifier)
            .screenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.body_physique_balance_title),
                background = Color.Transparent,
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::showBisInfo) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.cd_body_bis_info),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            when (state.loadState) {
                BodyLoadState.Error -> PhysiqueBalancePlaceholder {
                    Text(
                        text = stringResource(R.string.body_error_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                    TextButton(onClick = viewModel::retry) {
                        Text(
                            text = stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                BodyLoadState.Loading, BodyLoadState.Ready -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                ) {
                    val loading = state.loadState == BodyLoadState.Loading

                    AppTabSelector(
                        tabs = BodyTimeRange.entries,
                        selectedTab = state.timeRange,
                        onTabSelected = viewModel::selectTimeRange,
                        tabLabel = { stringResource(it.labelRes) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                    BodyTrendChart(
                        loading = loading,
                        chart = state.chart,
                        timeRange = state.timeRange,
                        showSegmentLegend = true,
                        predictionActive = state.prediction is PhysiquePredictionState.Active,
                        status = { PhysiqueTrendStatus(state.prediction) },
                        footer = if (state.timeRange.predicts) null else {
                            { PhysiqueDriftContent(state) }
                        },
                    )

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                    if (state.timeRange.predicts) {
                        PhysiqueBalanceSummary(state = state, loading = loading)
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                        PhysiqueEstimateCard(prediction = state.prediction, loading = loading)
                        PhysiqueScanReminder(prediction = state.prediction)
                    }

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                    MedicalDisclaimerFooter()

                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                }
            }
        }
    }

    if (state.showBisInfo) BisInfoBottomSheet(onDismiss = viewModel::dismissBisInfo)
}

@Composable
private fun PhysiqueBalancePlaceholder(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_3xl)),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = { content() },
    )
}

@Composable
private fun PhysiqueTrendStatus(prediction: PhysiquePredictionState) {
    AnimatedContent(
        targetState = prediction,
        transitionSpec = {
            fadeIn(tween(AnimationConstants.TAB_CONTENT_FADE_IN_MS)) togetherWith
                    fadeOut(tween(AnimationConstants.TAB_CONTENT_FADE_OUT_MS))
        },
        label = "physiqueTrendStatus",
    ) { target ->
        when (target) {
            is PhysiquePredictionState.None -> PhysiqueStatusLine(
                iconRes = R.drawable.ic_info,
                text = stringResource(R.string.body_prediction_unavailable),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is PhysiquePredictionState.AwaitingSecondScan -> PhysiqueStatusLine(
                iconRes = R.drawable.ic_info,
                text = stringResource(R.string.body_prediction_awaiting_second_scan),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is PhysiquePredictionState.Overdue -> PhysiqueStatusLine(
                iconRes = R.drawable.ic_info,
                text = stringResource(R.string.body_prediction_overdue),
                tint = NocturnePulseTheme.extendedColors.chartOverdueTick,
            )

            else -> Spacer(Modifier)
        }
    }
}

@Composable
private fun PhysiqueStatusLine(iconRes: Int, text: String, tint: Color) {
    var wraps by remember(text) { mutableStateOf(false) }
    Row(verticalAlignment = if (wraps) Alignment.Top else Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
        Text(
            text = text,
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onTextLayout = { wraps = it.lineCount > 1 },
        )
    }
}

@Composable
private fun PhysiqueBalanceSummary(state: BodyState, loading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        PhysiqueMetricCard(
            title = stringResource(R.string.body_physique_drift),
            value = formatSignedValue(state.periodPhysiqueDrift),
            caption = state.periodPhysiqueDrift?.let { stringResource(state.timeRange.periodLabelRes) },
            highlighted = true,
            loading = loading,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        PhysiqueMetricCard(
            title = stringResource(R.string.body_next_scan),
            value = nextScanValue(state.prediction),
            caption = nextScanSuffix(state.prediction),
            loading = loading,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun PhysiqueDriftContent(state: BodyState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.body_physique_drift),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .alignByBaseline(),
        )
        Text(
            text = formatSignedValue(state.periodPhysiqueDrift)
                ?: stringResource(R.string.stat_unknown),
            style = MaterialTheme.typography.headlineMedium,
            color = if (state.periodPhysiqueDrift == null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.alignByBaseline(),
        )
        if (state.periodPhysiqueDrift != null) {
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
            Text(
                text = stringResource(state.timeRange.periodLabelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

@Composable
private fun PhysiqueEstimateCard(prediction: PhysiquePredictionState, loading: Boolean) {
    val active = prediction as? PhysiquePredictionState.Active
    BodyGlassCard(loading = loading) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.body_physique_estimate),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
            Text(
                text = formatScore(active?.fit) ?: stringResource(R.string.stat_unknown),
                style = MaterialTheme.typography.headlineMedium,
                color = if (active?.fit == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.alignByBaseline(),
            )
            if (active?.fit != null) {
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                Text(
                    text = stringResource(R.string.body_physique_fit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BodyMetricTile(
                label = stringResource(R.string.body_chart_legend_muscle),
                value = formatSignedValue(active?.leanAdvantage) ?: stringResource(R.string.stat_unknown),
                delta = null,
                tolerance = ChangeTolerances.PHYSIQUE_POINTS,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            BodyMetricTile(
                label = stringResource(R.string.body_chart_legend_fat),
                value = formatSignedValue(active?.fatAdvantage) ?: stringResource(R.string.stat_unknown),
                delta = null,
                tolerance = ChangeTolerances.PHYSIQUE_POINTS,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PhysiqueScanReminder(prediction: PhysiquePredictionState) {
    AnimatedContent(
        targetState = prediction is PhysiquePredictionState.Active,
        transitionSpec = {
            fadeIn(tween(AnimationConstants.TAB_CONTENT_FADE_IN_MS)) togetherWith
                    fadeOut(tween(AnimationConstants.TAB_CONTENT_FADE_OUT_MS))
        },
        label = "physiqueScanReminder",
    ) { active ->
        if (!active) return@AnimatedContent
        Column {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            BodyGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calendar),
                        contentDescription = null,
                        tint = NocturnePulseTheme.extendedColors.accentBlue,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
                    Text(
                        text = stringResource(R.string.body_prediction_scan_again),
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun nextScanValue(prediction: PhysiquePredictionState): String? {
    val days = prediction.daysToNextScan
    return when {
        prediction is PhysiquePredictionState.Overdue || days == 0 ->
            stringResource(R.string.body_next_scan_due_now)

        days != null -> days.toString()
        else -> null
    }
}

@Composable
private fun nextScanSuffix(prediction: PhysiquePredictionState): String? {
    val days = prediction.daysToNextScan
    return if (days != null && days > 0) stringResource(R.string.body_next_scan_days_suffix)
    else null
}

@Composable
private fun formatSignedValue(value: Float?): String? {
    if (value == null) return null
    return String.format(LocalLocale.current.platformLocale, "%+.1f", value)
}

@Composable
private fun formatScore(value: Float?): String? {
    if (value == null) return null
    return String.format(LocalLocale.current.platformLocale, "%.1f", value)
}
