package com.hexis.bi.ui.main.body.components

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import com.hexis.bi.R
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppVerticalGradientDivider
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.body.BodyComposition
import com.hexis.bi.ui.main.body.BodyMassUnit
import com.hexis.bi.utils.kgToLb

@Composable
internal fun BodyCompositionCard(
    composition: BodyComposition,
    massUnit: BodyMassUnit,
    isMetric: Boolean,
    onMassUnitChange: (BodyMassUnit) -> Unit,
    onBisInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.body_composition_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            BodyMassUnitToggle(
                selected = massUnit,
                isMetric = isMetric,
                onSelected = onMassUnitChange,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            BodyMetricTile(
                label = stringResource(R.string.body_metric_body_fat),
                value = formatMassMetric(
                    percentage = composition.fatPercentage,
                    massKg = composition.fatMassKg,
                    massUnit = massUnit,
                    isMetric = isMetric,
                ),
                delta = pickDelta(
                    massUnit,
                    composition.deltaFatPercentage,
                    composition.deltaFatMassKg,
                    isMetric
                ),
                decreaseIsPositive = true,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            BodyMetricTile(
                label = stringResource(R.string.body_metric_muscle_mass),
                value = formatMassMetric(
                    percentage = composition.muscleMassPercentage,
                    massKg = composition.muscleMassKg,
                    massUnit = massUnit,
                    isMetric = isMetric,
                ),
                delta = pickDelta(
                    massUnit,
                    composition.deltaMuscleMassPercentage,
                    composition.deltaMuscleMassKg,
                    isMetric
                ),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        AppHorizontalGradientDivider()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            BodyMetricTile(
                label = stringResource(R.string.body_metric_weight),
                value = formatWeight(composition.weightKg, isMetric),
                delta = composition.deltaWeightKg?.let { if (isMetric) it else it.kgToLb() },
                decreaseIsPositive = true,
                modifier = Modifier.weight(1f),
            )
            AppVerticalGradientDivider()
            BodyMetricTile(
                label = stringResource(R.string.body_metric_bis),
                value = formatBis(composition.bisScore),
                delta = composition.deltaBisScore,
                modifier = Modifier.weight(1f),
                trailingIcon = { BodyInfoIconButton(onClick = onBisInfoClick) },
            )
        }
    }
}

@Composable
private fun BodyMassUnitToggle(
    selected: BodyMassUnit,
    isMetric: Boolean,
    onSelected: (BodyMassUnit) -> Unit,
) {
    val massLabel = stringResource(if (isMetric) R.string.unit_kg else R.string.unit_lb)
    BodySegmentedToggleTrack {
        BodySegmentedToggleChip(
            label = stringResource(R.string.unit_percent),
            isSelected = selected == BodyMassUnit.Percent,
            onClick = { onSelected(BodyMassUnit.Percent) },
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
        BodySegmentedToggleChip(
            label = massLabel,
            isSelected = selected == BodyMassUnit.Mass,
            onClick = { onSelected(BodyMassUnit.Mass) },
        )
    }
}

@Composable
private fun formatMassMetric(
    percentage: Float?,
    massKg: Float?,
    massUnit: BodyMassUnit,
    isMetric: Boolean,
): String {
    val unknown = stringResource(R.string.stat_unknown)
    return when (massUnit) {
        BodyMassUnit.Percent -> percentage?.let { String.format(java.util.Locale.US, "%.1f%%", it) }
            ?: unknown

        BodyMassUnit.Mass -> {
            val v = massKg ?: return unknown
            val converted = if (isMetric) v else v.kgToLb()
            val unit = stringResource(if (isMetric) R.string.unit_kg else R.string.unit_lb)
            String.format(java.util.Locale.US, "%.1f %s", converted, unit)
        }
    }
}

@Composable
private fun formatWeight(weightKg: Float?, isMetric: Boolean): String {
    val unknown = stringResource(R.string.stat_unknown)
    val w = weightKg ?: return unknown
    val converted = if (isMetric) w else w.kgToLb()
    val unit = stringResource(if (isMetric) R.string.unit_kg else R.string.unit_lb)
    return String.format(java.util.Locale.US, "%.1f %s", converted, unit)
}

@Composable
private fun formatBis(bis: Float?): String {
    val unknown = stringResource(R.string.stat_unknown)
    val v = bis ?: return unknown
    return String.format(java.util.Locale.US, "%.1f", v)
}

private fun pickDelta(
    massUnit: BodyMassUnit,
    deltaPercent: Float?,
    deltaMassKg: Float?,
    isMetric: Boolean,
): Float? = when (massUnit) {
    BodyMassUnit.Percent -> deltaPercent
    BodyMassUnit.Mass -> deltaMassKg?.let { if (isMetric) it else it.kgToLb() }
}
