package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.sleep.components.SleepBarChartCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStagesWeeklyCard

@Composable
fun SleepSummaryContent(
    modifier: Modifier = Modifier,
    state: SleepState,
    onInfoClick: () -> Unit,
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

    SleepBarChartCard(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

    SleepStagesWeeklyCard(
        stages = state.weeklyStages,
        avgSleepMinutes = state.avgSleepMinutes,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SleepRecoveryBanner(onInfoClick = onInfoClick)
}
