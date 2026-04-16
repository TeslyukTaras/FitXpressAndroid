package com.hexis.bi.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppSlider

@Composable
fun HeathParametersSection(
    params: HealthParameters,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        // Units toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = dimensionResource(R.dimen.spacer_l),
                    horizontal = dimensionResource(R.dimen.spacer_xs)
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.edit_profile_units),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(dimensionResource(R.dimen.spacer_xxs))
            ) {
                val selectedBgColor = MaterialTheme.colorScheme.surfaceVariant

                Text(
                    text = stringResource(R.string.edit_profile_metric),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (params.isMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (params.isMetric) selectedBgColor else MaterialTheme.colorScheme.background)
                        .clickable(onClick = onSelectMetric)
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_xxs)
                        )
                )

                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xxs)))

                Text(
                    text = stringResource(R.string.edit_profile_imperial),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!params.isMetric) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (!params.isMetric) selectedBgColor else MaterialTheme.colorScheme.background)
                        .clickable(onClick = onSelectImperial)
                        .padding(
                            horizontal = dimensionResource(R.dimen.spacer_s),
                            vertical = dimensionResource(R.dimen.spacer_xxs)
                        )
                )
            }
        }

        MeasurementSlider(
            label = stringResource(R.string.edit_profile_height),
            valueText = if (params.isMetric) stringResource(
                R.string.unit_height_cm,
                params.heightDisplayValue
            )
            else stringResource(R.string.unit_height_ft_in, params.heightFeet, params.heightInches),
            value = params.heightSliderValue,
            valueRange = params.heightSliderRange,
            onValueChange = onHeightChange,
        )

        MeasurementSlider(
            label = stringResource(R.string.edit_profile_weight),
            valueText = if (params.isMetric) stringResource(
                R.string.unit_weight_kg,
                params.weightDisplayValue
            )
            else stringResource(R.string.unit_weight_lb, params.weightDisplayValue),
            value = params.weightSliderValue,
            valueRange = params.weightSliderRange,
            onValueChange = onWeightChange,
        )
    }
}

@Composable
private fun MeasurementSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            )
        }
        AppSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
