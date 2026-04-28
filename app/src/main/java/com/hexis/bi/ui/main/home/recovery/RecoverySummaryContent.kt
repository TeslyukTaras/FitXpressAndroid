package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.recovery.components.RecoveryBarChart
import com.hexis.bi.ui.main.home.recovery.components.RecoveryScoreCards

@Composable
fun RecoverySummaryContent(
    state: RecoveryState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRetry: () -> Unit = {},
) {
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = state.weekLabel,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        canGoNext = state.canGoNextWeek,
    )

    when (state.summaryLoadState) {
        RecoveryLoadState.Loading -> RecoveryLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        RecoveryLoadState.Error -> RecoveryLoadPlaceholder {
            Text(
                text = stringResource(R.string.recovery_error_title),
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

        RecoveryLoadState.Ready -> RecoverySummaryReady(state = state)
    }
}

@Composable
private fun RecoverySummaryReady(state: RecoveryState) {
    RecoveryBarChart(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

    RecoveryScoreCards(
        avgScore = state.avgScore,
        trend = state.trend,
    )
}
