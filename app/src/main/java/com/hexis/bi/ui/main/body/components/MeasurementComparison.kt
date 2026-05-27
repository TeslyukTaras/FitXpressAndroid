package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.hexis.bi.utils.constants.BodyVisualConstants.CM_VALUE_FORMAT
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

internal fun measurementValue(
    measurements: Map<String, Float>,
    region: BodyMeasurementRegion,
): Float? = BodyMeasurementKeys.valueFor(measurements, region)
