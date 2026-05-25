package com.hexis.bi.ui.main.body

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.body.components.BisInfoBottomSheet
import com.hexis.bi.ui.main.body.components.BodyTrendChart
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.BodyConstants
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@Composable
fun PhysiqueBalanceScreen(
    onBack: () -> Unit,
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
                    BodyLoadState.Loading -> PhysiqueBalancePlaceholder {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

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

                    BodyLoadState.Ready -> Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                    ) {
                        BodyTrendChart(
                            chart = state.chart,
                            timeRange = state.timeRange,
                            onTimeRangeChange = viewModel::selectTimeRange,
                            showSegmentLegend = true,
                        )

                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                        PhysiqueBalanceSummary(state = state)
                        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))
                    }
                }
            }
        }

        if (state.showBisInfo) BisInfoBottomSheet(onDismiss = viewModel::dismissBisInfo)
    }
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
private fun PhysiqueBalanceSummary(state: BodyState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
    ) {
        PhysiqueMetricCard(
            title = stringResource(R.string.body_physique_drift),
            value = formatSignedValue(physiqueDrift(state)),
            caption = stringResource(R.string.body_physique_drift_period),
            highlighted = true,
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(0.55f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_s)),
        ) {
            PhysiqueMetricCard(
                title = stringResource(R.string.body_next_scan),
                value = formatNextScanDays(state.composition.timestamp),
                valueSuffix = if (state.composition.timestamp > 0L) stringResource(R.string.body_next_scan_days_suffix) else null,
                compact = true,
            )
            PhysiqueMetricCard(
                title = stringResource(R.string.body_next_scan_estimate),
                value = formatEstimate(state.composition),
                caption = stringResource(R.string.body_physique_fit),
                compact = true,
            )
        }
    }
}

@Composable
private fun PhysiqueMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    valueSuffix: String? = null,
    highlighted: Boolean = false,
    compact: Boolean = false,
) {
    BodyGlassCard(
        modifier = modifier,
        highlighted = highlighted
    ) {
        if (compact) {
            // Compact cards keep label and value on one row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleLarge,
                            color = DarkTheme.extendedColors.positive,
                        )
                        if (valueSuffix != null) {
                            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                            Text(
                                text = valueSuffix,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                    if (caption != null) {
                        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (highlighted) TextAlign.Center else TextAlign.Start,
                modifier = if (highlighted) Modifier.fillMaxWidth() else Modifier,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = if (highlighted) Modifier.align(Alignment.CenterHorizontally) else Modifier,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (highlighted) DarkTheme.extendedColors.positive
                    else MaterialTheme.colorScheme.primary
                )
                if (valueSuffix != null) {
                    Spacer(Modifier.size(dimensionResource(R.dimen.spacer_xxs)))
                    Text(
                        text = valueSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (caption != null) Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (highlighted) TextAlign.Center else TextAlign.Start,
                modifier = if (highlighted) Modifier.fillMaxWidth() else Modifier,
            )
        }
    }
}

// Summary drift is the blended score change.
private fun physiqueDrift(state: BodyState): Float? = state.composition.deltaBisScore

@Composable
private fun formatSignedValue(value: Float?): String {
    if (value == null) return stringResource(R.string.stat_unknown)
    return String.format(Locale.US, "%+.1f", value)
}

@Composable
private fun formatNextScanDays(timestamp: Long): String {
    if (timestamp <= 0L) return stringResource(R.string.stat_unknown)
    val zone = ZoneId.systemDefault()
    val scanDate = LocalDate.ofInstant(Date(timestamp).toInstant(), zone)
    val nextScanDate = scanDate.plusDays(BodyConstants.SCAN_CADENCE_DAYS)
    val days = ChronoUnit.DAYS.between(LocalDate.now(zone), nextScanDate)
        .coerceAtLeast(0)
        .toInt()
    return days.toString()
}

@Composable
private fun formatEstimate(composition: BodyComposition): String {
    val estimate = composition.bisScore?.let { score ->
        (score + (composition.deltaBisScore ?: 0f)).coerceIn(1f, 10f)
    } ?: return stringResource(R.string.stat_unknown)
    return String.format(Locale.US, "%.1f", estimate)
}
