package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.hexis.bi.ui.main.home.sleep.components.SleepMetricsCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStatusCard
import com.hexis.bi.ui.main.home.sleep.components.SleepTimelineCard

@Composable
fun SleepDayContent(
    state: SleepState,
    onInfoClick: () -> Unit,
    onRetry: () -> Unit = {},
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    when (state.dayLoadState) {
        SleepLoadState.Loading -> SleepLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        SleepLoadState.Error -> SleepLoadPlaceholder {
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

        SleepLoadState.Ready -> SleepDayReady(state = state, onInfoClick = onInfoClick)
    }
}

@Composable
private fun SleepDayReady(state: SleepState, onInfoClick: () -> Unit) {
    SleepStatusCard(
        quality = state.sleepQuality,
        totalSleepMinutes = state.totalSleepMinutes,
        sleepGoalHours = state.sleepGoalHours,
        stages = state.stages,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SleepMetricsCard(
        restfulness = state.restfulness,
        restfulnessMax = state.restfulnessMax,
        hrv = state.hrv,
        restingHeartRate = state.restingHeartRate,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SleepTimelineCard(
        totalSleepMinutes = state.totalSleepMinutes,
        timeStartHour = state.timelineStartHour,
        timeEndHour = state.timelineEndHour,
        segments = state.timelineSegments,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))

    SleepRecoveryBanner(
        insightText = stringResource(state.insightRes),
        onInfoClick = onInfoClick,
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
