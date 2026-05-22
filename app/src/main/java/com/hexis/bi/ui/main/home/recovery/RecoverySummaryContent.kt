package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.recovery.components.RecoveryBarChart
import com.hexis.bi.ui.main.home.recovery.components.RecoveryScoreCards

@Composable
fun RecoverySummaryContent(
    state: RecoveryState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
    AppDateNavigator(
        label = state.weekLabel,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        canGoNext = state.canGoNextWeek,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    when (state.summaryLoadState) {
        RecoveryLoadState.Loading -> RecoveryLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        // No "cannot load" view: a failed/empty load falls through to the chart and cards.
        RecoveryLoadState.Ready,
        RecoveryLoadState.Error -> RecoverySummaryReady(state = state)
    }
}

@Composable
private fun RecoverySummaryReady(state: RecoveryState) {
    RecoveryBarChart(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    RecoveryScoreCards(
        avgScore = state.avgScore,
        trend = state.trend,
    )
}
