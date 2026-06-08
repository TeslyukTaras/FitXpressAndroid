package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.theme.MeasurementValueStyle
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.cmToInches
import com.hexis.bi.utils.constants.BodyVisualConstants.MEASUREMENT_VALUE_FORMAT
import kotlin.math.abs

@Composable
internal fun MeasurementDateHeader(
    leftLabel: String,
    leftDate: String?,
    rightLabel: String,
    rightDate: String?,
    modifier: Modifier = Modifier,
) {
    val missing = stringResource(R.string.body_visual_value_missing)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        MeasurementDateHeaderColumn(
            label = leftLabel,
            date = leftDate ?: missing,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))
        AppVerticalGradientDivider()
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

        MeasurementDateHeaderColumn(
            label = rightLabel,
            date = rightDate ?: missing,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MeasurementDateHeaderColumn(
    label: String,
    date: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = DarkTheme.extendedColors.accentBlue,
        )
    }
}

@Composable
internal fun MeasurementValueBlock(
    valueCm: Float?,
    deltaCm: Float?,
    isMetric: Boolean,
    modifier: Modifier = Modifier,
    decreaseIsPositive: Boolean = false,
    hideValue: Boolean = false,
) {
    val missing = stringResource(R.string.body_visual_value_missing)
    val unit = stringResource(if (isMetric) R.string.unit_cm else R.string.unit_in)
    val displayValue = valueCm?.let { if (isMetric) it else it.cmToInches() }
    val valueNumber = displayValue?.let { MEASUREMENT_VALUE_FORMAT.format(it) }

    Column(
        modifier = modifier,
        horizontalAlignment = if (hideValue) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (!hideValue) {
            if (valueNumber == null) {
                Text(
                    text = missing,
                    style = MeasurementValueStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(MeasurementValueStyle.toSpanStyle()) {
                            append(valueNumber)
                        }
                        append(" ")
                        append(unit)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        if (deltaCm != null) {
            val displayDelta = if (isMetric) deltaCm else deltaCm.cmToInches()
            val deltaRes = if (displayDelta >= 0f) R.string.body_visual_delta_increase
            else R.string.body_visual_delta_decrease
            val isPositiveChange = if (decreaseIsPositive) deltaCm < 0f else deltaCm > 0f
            val deltaText = stringResource(
                deltaRes,
                MEASUREMENT_VALUE_FORMAT.format(abs(displayDelta)),
                unit,
            )
            if (!hideValue) Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = deltaText,
                style = if (hideValue) MaterialTheme.typography.labelLarge
                else MaterialTheme.typography.bodyMedium,
                color = when {
                    deltaCm == 0f -> MaterialTheme.colorScheme.onSurfaceVariant
                    isPositiveChange -> DarkTheme.extendedColors.positive
                    else -> DarkTheme.extendedColors.negative
                },
                modifier = if (hideValue) {
                    Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs))
                } else {
                    Modifier
                },
            )
        }
    }
}

internal fun measurementValue(
    measurements: Map<String, Float>,
    region: BodyMeasurementRegion,
): Float? = BodyMeasurementKeys.valueFor(measurements, region)
