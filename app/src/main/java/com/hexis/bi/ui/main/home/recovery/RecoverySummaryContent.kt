package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = state.weekLabel,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        canGoNext = state.canGoNextWeek,
    )

    RecoveryBarChart(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

    RecoveryScoreCards(
        avgScore = state.avgScore,
        trendLabel = state.trendLabel,
        trendDescription = state.trendDescription,
    )
}
