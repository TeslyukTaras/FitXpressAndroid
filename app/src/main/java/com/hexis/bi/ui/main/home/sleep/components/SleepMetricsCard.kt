package com.hexis.bi.ui.main.home.sleep.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R

@Composable
fun SleepMetricsCard(
    restfulness: Int,
    restfulnessMax: Int,
    hrv: Int,
    restingHeartRate: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
        Text(
            text = stringResource(R.string.sleep_metrics_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        MetricRow(
            label = stringResource(R.string.sleep_metric_restfulness),
            value = "$restfulness/$restfulnessMax",
            unit = stringResource(R.string.unit_percent),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        MetricRow(
            label = stringResource(R.string.sleep_metric_hrv),
            value = hrv.toString(),
            unit = stringResource(R.string.unit_ms),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        MetricRow(
            label = stringResource(R.string.sleep_metric_resting_heart_rate),
            value = restingHeartRate.toString(),
            unit = stringResource(R.string.unit_bpm),
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )

        Text(
            text = buildAnnotatedString {
                append(value)
                append(" ")
                withStyle(
                    SpanStyle(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Normal,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                ) { append(unit) }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
