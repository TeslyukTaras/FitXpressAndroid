package com.hexis.bi.ui.main.home.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.activity.components.ActivityGoalRow
import com.hexis.bi.ui.main.home.activity.components.ActivityProgressCard
import com.hexis.bi.ui.main.home.activity.components.ActivityStepsBarChart
import com.hexis.bi.utils.constants.ActivityConstants

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

    ActivityStepsBarChart(
        entries = state.hourlyBars,
        totalValue = state.currentSteps,
        baseYMax = ActivityConstants.STEP_GRID_MAX,
        yGridStep = ActivityConstants.STEP_GRID_STEP,
        title = stringResource(R.string.activity_steps_timeline),
        barGap = dimensionResource(R.dimen.spacer_2xs),
    )

    // Day-specific edge labels (start/end of 24h window)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacer_xxs)),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.width(dimensionResource(R.dimen.recovery_y_axis_width) + dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.activity_time_start),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.activity_time_end),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
