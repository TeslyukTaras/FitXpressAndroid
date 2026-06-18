package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppVerticalGradientDivider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.recovery.RecoveryMetric
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus

@Composable
fun RecoveryStatusCard(
    score: Int,
    status: RecoveryStatus,
    metrics: List<RecoveryMetric>,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier, contentPadding = PaddingValues(
            top = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_l),
            start = dimensionResource(R.dimen.spacer_m),
            end = dimensionResource(R.dimen.spacer_m)
        )
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recovery_state_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(status.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        // Circle + Metrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RecoveryCircularProgress(
                score = score,
                status = status,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            AppVerticalGradientDivider()

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                metrics.forEach { metric ->
                    MetricRow(metric = metric)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(metric: RecoveryMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(metric.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val onBackground = MaterialTheme.colorScheme.onBackground
        val unitColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = onBackground)) { append(metric.value) }
                metric.unit?.let { unit ->
                    append(" ")
                    withStyle(SpanStyle(color = unitColor)) { append(unit) }
                }
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
