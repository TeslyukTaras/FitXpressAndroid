package com.hexis.bi.ui.main.home.recovery.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.recovery.RecoveryMetric
import com.hexis.bi.ui.main.home.recovery.RecoveryStatus

@Composable
fun RecoveryStatusCard(
    score: Int,
    status: RecoveryStatus,
    metrics: List<RecoveryMetric>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.recovery_status_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(status.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = status.color,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

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
            
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.secondaryFixed,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_l)))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = metric.value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
