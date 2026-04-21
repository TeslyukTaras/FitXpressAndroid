package com.hexis.bi.ui.main.home.activity

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.activity.components.ActivityGoalRow
import com.hexis.bi.ui.main.home.activity.components.ActivityProgressCard
import com.hexis.bi.ui.main.home.activity.components.ActivityStepsTimeline

@Composable
fun ActivityDayContent(
    state: ActivityState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = state.dateLabel,
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        canGoNext = state.canGoNextDay,
    )

    ActivityGoalRow(
        currentSteps = state.currentSteps,
        stepsGoal = state.stepsGoal,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    ActivityProgressCard(
        progressPercent = state.progressPercent,
        stepsProgress = if (state.stepsGoal > 0)
            (state.currentSteps.toFloat() / state.stepsGoal).coerceIn(0f, 1f) else 0f,
        distanceProgress = if (state.distanceGoal > 0f)
            (state.distanceDisplay / state.distanceGoal).coerceIn(0f, 1f) else 0f,
        caloriesProgress = if (state.caloriesGoal > 0)
            (state.calories.toFloat() / state.caloriesGoal).coerceIn(0f, 1f) else 0f,
        metrics = listOf(
            ActivityMetric(
                R.string.activity_metric_calories,
                "${state.calories}",
                stringResource(R.string.activity_unit_cal),
            ),
            ActivityMetric(
                R.string.activity_metric_distance,
                "%.1f".format(state.distanceDisplay),
                stringResource(state.distanceUnitRes),
            ),
            ActivityMetric(
                R.string.activity_metric_steps,
                "%,d".format(state.currentSteps),
                stringResource(R.string.activity_unit_steps),
            ),
        ),
        onInfoClick = onInfoClick,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    ActivityStepsTimeline(
        entries = state.hourlySteps,
        totalSteps = state.currentSteps,
    )
}
