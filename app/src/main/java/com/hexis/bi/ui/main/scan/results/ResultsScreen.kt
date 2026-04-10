package com.hexis.bi.ui.main.scan.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Lime200
import com.hexis.bi.ui.theme.Red100
import org.koin.androidx.compose.koinViewModel

@Composable
fun ResultsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_results_title),
                onBack = onBack,
                background = MaterialTheme.colorScheme.surfaceVariant,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            ResultsTabSelector(
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            BodyVisualizationCard()

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            ColorAnalysisCard(
                enabled = state.colorAnalysisEnabled,
                onToggle = viewModel::toggleColorAnalysis,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            MeasurementsCard(measurements = state.measurements)

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        }
    }
}

@Composable
private fun ResultsTabSelector(
    selectedTab: ResultsTab,
    onTabSelected: (ResultsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(dimensionResource(R.dimen.spacer_xxs)),
    ) {
        ResultsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.background(Lime200, CircleShape)
                        else Modifier
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = dimensionResource(R.dimen.spacer_2xs)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun BodyVisualizationCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = dimensionResource(R.dimen.spacer_m)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.img_scan_preview),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.scan_results_body_card_height)),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xl)),
        ) {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .rotate(180f),
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }
    }
}

@Composable
private fun ColorAnalysisCard(
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = dimensionResource(R.dimen.padding_medium),
                vertical = dimensionResource(R.dimen.spacer_m),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.scan_results_color_analysis),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.scan_results_color_analysis_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        AppSwitch(
            checked = enabled,
            onCheckedChange = { onToggle() },
        )
    }
}

@Composable
private fun MeasurementsCard(
    measurements: List<MeasurementRow>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_measurements_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {},
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.cd_info),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        // Sub-header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacer_xs)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_body_part),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1.2f),
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_today),
                date = stringResource(R.string.scan_results_today_date),
                modifier = Modifier.weight(1f),
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_previous),
                date = stringResource(R.string.scan_results_previous_date),
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        measurements.forEachIndexed { index, row ->
            MeasurementTableRow(row = row)
            if (index < measurements.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun HeaderCell(
    title: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MeasurementTableRow(
    row: MeasurementRow,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_s)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(row.bodyPartRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.2f),
        )
        ValueCell(value = row.today, modifier = Modifier.weight(1f))
        ValueCell(value = row.previous, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ValueCell(
    value: MeasurementValue,
    modifier: Modifier = Modifier,
) {
    val isPositive = value.deltaCm > 0
    val isNegative = value.deltaCm < 0
    val deltaColor = when {
        isPositive -> Green
        isNegative -> Red100
        else -> MaterialTheme.colorScheme.secondary
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.scan_measurement_value_cm, value.cm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.scan_measurement_delta_cm, value.deltaCm),
                style = MaterialTheme.typography.bodySmall,
                color = deltaColor,
            )
            if (value.deltaCm != 0f) {
                Icon(
                    painter = painterResource(
                        if (isPositive) R.drawable.ic_trend_up else R.drawable.ic_trend_down
                    ),
                    contentDescription = null,
                    tint = deltaColor,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_small)),
                )
            }
        }
    }
}
