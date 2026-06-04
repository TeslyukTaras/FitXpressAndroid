package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.body.BodyVisualMode
import com.hexis.bi.ui.main.body.VisualState
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.BodyVisualConstants
import com.hexis.bi.utils.constants.BodyVisualConstants.FULL_BODY_MEASUREMENT_ROWS
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun VisualSummaryCard(
    state: VisualState,
    selectedScanLabel: String,
    shortDateFormatter: SimpleDateFormat,
    isMetric: Boolean,
    onModeSelected: (BodyVisualMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val part = state.selectedBodyPart

    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.body_visual_part_bullet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = stringResource(BodyVisualConstants.visualHeaderLabelRes(part)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            BodySegmentedToggleTrack {
                BodySegmentedToggleChip(
                    label = stringResource(R.string.body_visual_mode_base),
                    isSelected = state.mode == BodyVisualMode.Base,
                    onClick = { onModeSelected(BodyVisualMode.Base) },
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                BodySegmentedToggleChip(
                    label = stringResource(R.string.body_visual_mode_color),
                    isSelected = state.mode == BodyVisualMode.Color,
                    onClick = { onModeSelected(BodyVisualMode.Color) },
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        if (part == BodyMeasurementRegion.FullBody) FullBodyMeasurementList(
            state = state,
            selectedScanLabel = selectedScanLabel,
            shortDateFormatter = shortDateFormatter,
            isMetric = isMetric,
        )
        else SelectedPartMeasurementRow(
            part = part,
            state = state,
            selectedScanLabel = selectedScanLabel,
            shortDateFormatter = shortDateFormatter,
            isMetric = isMetric,
        )
    }
}

@Composable
private fun FullBodyMeasurementList(
    state: VisualState,
    selectedScanLabel: String,
    shortDateFormatter: SimpleDateFormat,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MeasurementDateHeader(
            leftLabel = selectedScanLabel,
            leftDate = state.latestScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
            rightLabel = stringResource(R.string.body_visual_previous_scan),
            rightDate = state.previousScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
        )

        FULL_BODY_MEASUREMENT_ROWS
            .forEachIndexed { index, row ->
                if (index > 0) AppHorizontalGradientDivider(
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_m)),
                )
                else Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                MeasurementComparisonRow(
                    region = row.region,
                    label = stringResource(row.labelRes),
                    latestCm = measurementValue(state.latestMeasurements, row.region),
                    previousCm = measurementValue(state.previousMeasurements, row.region),
                    beforePreviousCm = measurementValue(
                        state.beforePreviousMeasurements,
                        row.region
                    ),
                    isMetric = isMetric,
                )
            }
    }
}

@Composable
private fun SelectedPartMeasurementRow(
    part: BodyMeasurementRegion,
    state: VisualState,
    selectedScanLabel: String,
    shortDateFormatter: SimpleDateFormat,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        MeasurementColumn(
            title = selectedScanLabel,
            date = state.latestScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
            valueCm = measurementValue(state.latestMeasurements, part),
            deltaCm = measurementDelta(part, state),
            isMetric = isMetric,
            decreaseIsPositive = part.decreaseIsPositive,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
        AppVerticalGradientDivider()
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

        MeasurementColumn(
            title = stringResource(R.string.body_visual_previous_scan),
            date = state.previousScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
            valueCm = measurementValue(state.previousMeasurements, part),
            deltaCm = previousMeasurementDelta(part, state),
            isMetric = isMetric,
            decreaseIsPositive = part.decreaseIsPositive,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MeasurementComparisonRow(
    region: BodyMeasurementRegion,
    label: String,
    latestCm: Float?,
    previousCm: Float?,
    beforePreviousCm: Float?,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.body_visual_part_bullet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            MeasurementValueBlock(
                valueCm = latestCm,
                deltaCm = if (latestCm != null && previousCm != null) latestCm - previousCm else null,
                isMetric = isMetric,
                decreaseIsPositive = region.decreaseIsPositive,
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            MeasurementValueBlock(
                valueCm = previousCm,
                deltaCm = if (previousCm != null && beforePreviousCm != null)
                    previousCm - beforePreviousCm else null,
                isMetric = isMetric,
                decreaseIsPositive = region.decreaseIsPositive,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun measurementDelta(
    region: BodyMeasurementRegion,
    state: VisualState,
): Float? {
    val latestCm = measurementValue(state.latestMeasurements, region) ?: return null
    val previousCm = measurementValue(state.previousMeasurements, region) ?: return null
    return latestCm - previousCm
}

private fun previousMeasurementDelta(
    region: BodyMeasurementRegion,
    state: VisualState,
): Float? {
    val previousCm = measurementValue(state.previousMeasurements, region) ?: return null
    val beforePreviousCm = measurementValue(state.beforePreviousMeasurements, region) ?: return null
    return previousCm - beforePreviousCm
}

@Composable
private fun MeasurementColumn(
    title: String,
    date: String?,
    valueCm: Float?,
    deltaCm: Float?,
    isMetric: Boolean,
    decreaseIsPositive: Boolean,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(R.string.body_visual_value_missing)
    val accentColor = DarkTheme.extendedColors.accentBlue
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = date ?: missing,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        MeasurementValueBlock(
            valueCm = valueCm,
            deltaCm = deltaCm,
            isMetric = isMetric,
            decreaseIsPositive = decreaseIsPositive,
        )
    }
}
