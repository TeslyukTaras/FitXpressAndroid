package com.hexis.bi.ui.main.scan.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppSwitch
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.ui.theme.Lime100
import com.hexis.bi.ui.theme.Red100
import com.hexis.bi.utils.constants.ProfileConstants
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
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            ResultsTabSelector(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                selectedTab = state.selectedTab,
                onTabSelected = viewModel::selectTab,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            BodyVisualizationCard()

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            ColorAnalysisCard(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                enabled = state.colorAnalysisEnabled,
                onToggle = viewModel::toggleColorAnalysis,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            MeasurementsCard(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                measurements = state.measurements,
                isMetric = state.isMetric,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        }
    }
}

@Composable
private fun ResultsTabSelector(
    modifier: Modifier = Modifier,
    selectedTab: ResultsTab,
    onTabSelected: (ResultsTab) -> Unit,
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
                        if (isSelected) Modifier.background(Lime100, CircleShape)
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
    Box(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.img_3d_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ColorAnalysisCard(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.scan_results_color_analysis),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.scan_results_color_analysis_subtitle),
                style = MaterialTheme.typography.bodyMedium,
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
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacer_m),
                    top = dimensionResource(R.dimen.spacer_xxs),
                    end = dimensionResource(R.dimen.spacer_xxs),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_measurements_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.cd_info),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        // Sub-header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = dimensionResource(R.dimen.spacer_m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.scan_results_body_part),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primaryFixed,
                modifier = Modifier.weight(1.2f),
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_today),
                date = stringResource(R.string.scan_results_today_date),
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )
            HeaderCell(
                title = stringResource(R.string.scan_results_previous),
                date = stringResource(R.string.scan_results_previous_date),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
            color = MaterialTheme.colorScheme.secondaryFixed
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        measurements.forEachIndexed { index, row ->
            MeasurementTableRow(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
                row = row,
                isMetric = isMetric,
            )
            if (index < measurements.lastIndex) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m)),
                    color = MaterialTheme.colorScheme.secondaryFixed
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
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
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primaryFixed,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = date,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun MeasurementTableRow(
    modifier: Modifier = Modifier,
    row: MeasurementRow,
    isMetric: Boolean,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(row.bodyPartRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1.2f)
                .padding(start = dimensionResource(R.dimen.padding_medium)),
        )
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryFixed,
        )
        ValueCell(value = row.today, isMetric = isMetric, modifier = Modifier.weight(1f))
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.secondaryFixed,
        )
        ValueCell(value = row.previous, isMetric = isMetric, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ValueCell(
    value: MeasurementValue,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    val deltaColor = when (value.change) {
        MeasurementChange.Positive -> Green
        MeasurementChange.Negative -> Red100
        null -> MaterialTheme.colorScheme.primaryFixed
    }

    val unit = stringResource(if (isMetric) R.string.unit_cm else R.string.unit_in)
    val deltaValue = if (isMetric) value.deltaCm else value.deltaCm / ProfileConstants.CM_TO_IN
    val deltaText = when {
        value.deltaCm > 0 -> stringResource(R.string.format_delta_up, deltaValue, unit)
        value.deltaCm < 0 -> stringResource(R.string.format_delta_down, deltaValue, unit)
        else -> stringResource(R.string.format_delta_neutral, deltaValue, unit)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isMetric) {
            val valueText = stringResource(R.string.format_value_decimal, value.cm)
            val unitText = stringResource(R.string.unit_cm)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(valueText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(unitText)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val (feet, inches) = ProfileConstants.cmToFeetAndInches(value.cm)
            val feetText = stringResource(R.string.format_value_int, feet)
            val ftUnit = stringResource(R.string.unit_ft)
            val inchesText = stringResource(R.string.format_value_decimal, inches)
            val inUnit = stringResource(R.string.unit_in)
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(feetText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(ftUnit)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                        append(inchesText)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) {
                        append(inUnit)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))
        Text(
            text = deltaText,
            style = MaterialTheme.typography.labelMedium,
            color = deltaColor,
        )
    }
}
