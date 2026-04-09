package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.sleep.components.SleepMetricsCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStatusCard
import com.hexis.bi.ui.main.home.sleep.components.SleepTimelineCard

@Composable
fun SleepDayContent(
    state: SleepState,
    onInfoClick: () -> Unit,
) {
    //TODO check if we need goal tracking here
    //val goalMinutes = state.sleepGoalHours * 60
    //val progress = if (goalMinutes > 0) state.totalSleepMinutes / goalMinutes.toFloat() else 0f

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

    SleepRecoveryBanner(onInfoClick = onInfoClick)
}
