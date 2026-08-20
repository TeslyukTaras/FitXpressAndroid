package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.hexis.bi.BuildConfig
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.intelligence.EngineFindingsSection
import com.hexis.bi.ui.main.home.intelligence.NightComparisonSection
import com.hexis.bi.ui.main.home.intelligence.EngineDebugSection
import com.hexis.bi.ui.main.home.intelligence.EngineDebugPresentation
import com.hexis.bi.ui.main.home.intelligence.InsightsUpdatingHeader
import com.hexis.bi.ui.main.home.sleep.components.SleepMetricsCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStatusCard
import com.hexis.bi.ui.main.home.sleep.components.SleepTimelineCard
import com.hexis.bi.utils.constants.FindingWindows

@Composable
fun SleepDayContent(
    state: SleepState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onRetry: () -> Unit = {},
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
    AppDateNavigator(
        modifier = Modifier,
        label = state.dayLabel,
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        canGoNext = state.canGoNextDay,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    if (state.dayLoadState == SleepLoadState.Error) {
        SleepLoadPlaceholder {
            Text(
                text = stringResource(R.string.sleep_error_title),
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
    } else {
        SleepDayReady(state = state, onInfoClick = onInfoClick)
    }
}

@Composable
private fun SleepDayReady(state: SleepState, onInfoClick: () -> Unit) {
    InsightsUpdatingHeader(visible = state.insightsUpdating)

    NightComparisonSection(
        comparisons = state.dayComparisons,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    SleepStatusCard(
        loading = state.dayLoadState == SleepLoadState.Loading,
        totalSleepMinutes = state.totalSleepMinutes,
        sleepGoalHours = state.sleepGoalHours,
        stages = state.stages,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    SleepTimelineCard(
        loading = state.dayLoadState == SleepLoadState.Loading,
        totalSleepMinutes = state.totalSleepMinutes,
        timeStartHour = state.timelineStartHour,
        timeEndHour = state.timelineEndHour,
        segments = state.timelineSegments,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    SleepMetricsCard(
        loading = state.dayLoadState == SleepLoadState.Loading,
        hrv = state.hrv,
        restingHeartRate = state.restingHeartRate,
        hrvSeries = state.hrvSeries,
        rhrSeries = state.rhrSeries,
        timeStartHour = state.timelineStartHour,
        timeEndHour = state.timelineEndHour,
    )

    if (BuildConfig.DEBUG) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        SleepRecoveryBanner(
            insightText = stringResource(state.insightRes),
            onInfoClick = onInfoClick,
        )
    }

    EngineDebugSection(
        info = state.findings.debugForWindow(FindingWindows.SLEEP_DAY),
        presentation = EngineDebugPresentation.COMPACT,
    )
}

@Composable
fun SleepLoadPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_3xl)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}
