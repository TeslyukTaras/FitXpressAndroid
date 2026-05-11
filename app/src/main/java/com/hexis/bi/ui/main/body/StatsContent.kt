package com.hexis.bi.ui.main.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.main.body.components.BodyCompositionCard
import com.hexis.bi.ui.main.body.components.BodyTrendChart

@Composable
internal fun StatsContent(
    state: BodyState,
    onMassUnitChange: (BodyMassUnit) -> Unit,
    onTimeRangeChange: (BodyTimeRange) -> Unit,
    onBisInfoClick: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state.loadState) {
        BodyLoadState.Loading -> StatsPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        BodyLoadState.Error -> StatsPlaceholder {
            Text(
                text = stringResource(R.string.body_error_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            TextButton(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.action_retry),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        BodyLoadState.Ready -> {
            Column(modifier = Modifier.fillMaxWidth()) {
                BodyCompositionCard(
                    composition = state.composition,
                    massUnit = state.massUnit,
                    isMetric = state.isMetric,
                    onMassUnitChange = onMassUnitChange,
                    onBisInfoClick = onBisInfoClick,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

                BodyTrendChart(
                    chart = state.chart,
                    timeRange = state.timeRange,
                    onTimeRangeChange = onTimeRangeChange,
                    massUnit = state.massUnit,
                    isMetric = state.isMetric,
                )
            }
        }
    }
}

@Composable
private fun StatsPlaceholder(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_3xl)),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
