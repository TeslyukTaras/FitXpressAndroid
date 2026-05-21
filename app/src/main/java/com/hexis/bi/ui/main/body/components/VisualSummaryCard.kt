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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.domain.body.BodyMeasurementKeys
import com.hexis.bi.domain.body.BodyMeasurementRegion
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.body.VisualState
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.BodyVisualConstants
import com.hexis.bi.utils.constants.BodyVisualConstants.CM_VALUE_FORMAT
import com.hexis.bi.utils.constants.BodyVisualConstants.FULL_BODY_MEASUREMENT_ROWS
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs

@Composable
internal fun VisualSummaryCard(
    state: VisualState,
    selectedScanLabel: String,
    shortDateFormatter: SimpleDateFormat,
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
                    isSelected = true,
                    onClick = {},
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                BodySegmentedToggleChip(
                    label = stringResource(R.string.body_visual_mode_color),
                    isSelected = false,
                    onClick = {},
                )
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        if (part == BodyMeasurementRegion.FullBody) FullBodyMeasurementList(
            state = state,
            selectedScanLabel = selectedScanLabel,
            shortDateFormatter = shortDateFormatter,
        )
        else SelectedPartMeasurementRow(
            part = part,
            state = state,
            selectedScanLabel = selectedScanLabel,
            shortDateFormatter = shortDateFormatter,
        )
    }
}

@Composable
private fun FullBodyMeasurementList(
    state: VisualState,
    selectedScanLabel: String,
    shortDateFormatter: SimpleDateFormat,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MeasurementDateHeader(
            selectedScanLabel = selectedScanLabel,
            latestDate = state.latestScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
            previousDate = state.previousScanTimestamp?.let { shortDateFormatter.format(Date(it)) },
        )

        FULL_BODY_MEASUREMENT_ROWS
            .forEachIndexed { index, row ->
                if (index > 0) AppHorizontalGradientDivider(
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_m)),
                )
                else Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                MeasurementComparisonRow(
                    label = stringResource(row.labelRes),
                    latestCm = measurementValue(state.latestMeasurements, row.region),
                    previousCm = measurementValue(state.previousMeasurements, row.region),
                    beforePreviousCm = measurementValue(
                        state.beforePreviousMeasurements,
                        row.region
                    ),
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
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MeasurementDateHeader(
    selectedScanLabel: String,
    latestDate: String?,
    previousDate: String?,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(R.string.body_visual_value_missing)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedScanLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = latestDate ?: missing,
                style = MaterialTheme.typography.bodySmall,
                color = DarkTheme.extendedColors.accentBlue,
            )
        }

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
        AppVerticalGradientDivider()
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.body_visual_previous_scan),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = previousDate ?: missing,
                style = MaterialTheme.typography.bodySmall,
                color = DarkTheme.extendedColors.accentBlue,
            )
        }
    }
}

@Composable
private fun MeasurementComparisonRow(
    label: String,
    latestCm: Float?,
    previousCm: Float?,
    beforePreviousCm: Float?,
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
                modifier = Modifier.weight(1f),
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
            AppVerticalGradientDivider()
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            MeasurementValueBlock(
                valueCm = previousCm,
                deltaCm = if (previousCm != null && beforePreviousCm != null)
                    previousCm - beforePreviousCm else null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MeasurementValueBlock(
    valueCm: Float?,
    deltaCm: Float?,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(R.string.body_visual_value_missing)
    val valueNumber = valueCm?.let { CM_VALUE_FORMAT.format(it) }
    val valueUnit = stringResource(R.string.body_visual_unit_cm)
    val deltaText = deltaCm?.let { d ->
        val deltaRes = if (d >= 0f) R.string.body_visual_delta_increase
        else R.string.body_visual_delta_decrease
        stringResource(deltaRes, CM_VALUE_FORMAT.format(abs(d)))
    }

    Column(modifier = modifier) {
        if (valueNumber == null) Text(
            text = missing,
            style = MeasurementValueStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        else {
            Text(
                text = buildAnnotatedString {
                    withStyle(MeasurementValueStyle.toSpanStyle()) {
                        append(valueNumber)
                    }
                    append(" ")
                    append(valueUnit)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (deltaText != null) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = deltaText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (deltaCm >= 0f) DarkTheme.extendedColors.positive
                else DarkTheme.extendedColors.negative,
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

private fun measurementValue(
    measurements: Map<String, Float>,
    region: BodyMeasurementRegion
): Float? = BodyMeasurementKeys.valueFor(measurements, region)

@Composable
private fun MeasurementColumn(
    title: String,
    date: String?,
    valueCm: Float?,
    deltaCm: Float?,
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
        MeasurementValueBlock(valueCm = valueCm, deltaCm = deltaCm)
    }
}
