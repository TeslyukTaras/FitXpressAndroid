package com.hexis.bi.ui.main.home.activity.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.activity.ActivityMetric
import com.hexis.bi.ui.main.home.activity.ActivityState
import com.hexis.bi.ui.main.home.activity.BarChartEntry
import com.hexis.bi.ui.theme.Gray300
import com.hexis.bi.utils.constants.ActivityConstants
import com.hexis.bi.utils.constants.MeasurementConstants

@Composable
fun ActivityDayDetail(
    state: ActivityState,
    steps: Int,
    distanceKm: Float,
    calories: Int,
    durationSeconds: Int,
    hourlyBars: List<BarChartEntry>,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val distanceDisplay =
        if (state.isMetric) distanceKm else distanceKm * MeasurementConstants.KM_TO_MI
    val distanceGoal = state.distanceGoal
    val progressPercent = if (state.stepsGoal > 0)
        ((steps.toFloat() / state.stepsGoal) * 100).toInt().coerceIn(0, 100) else 0

    BodyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_l),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_l),
        ),
    ) {
        ActivityStepsBarChart(
            entries = hourlyBars,
            totalValue = steps,
            baseYMax = ActivityConstants.STEP_GRID_MAX,
            yGridStep = ActivityConstants.STEP_GRID_STEP,
            title = stringResource(R.string.activity_steps_timeline),
            barGap = dimensionResource(R.dimen.activity_chart_bar_gap_compact),
            chartStartPadding = dimensionResource(R.dimen.activity_chart_start_padding_compact),
            chartEndPadding = dimensionResource(R.dimen.activity_chart_end_padding),
            goalValue = state.stepsGoal,
            xAxisStartLabel = stringResource(R.string.activity_time_start),
            xAxisEndLabel = stringResource(R.string.activity_time_end),
            xAxisEdgeLabelColor = Gray300,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        ActivityMetricsGrid(
            cells = listOfNotNull(
                ActivityGridCell(
                    label = stringResource(R.string.activity_summary_steps),
                    segments = listOf(
                        MetricSegment(
                            "%,d".format(steps),
                            stringResource(R.string.activity_unit_steps_full),
                        ),
                    ),
                ),
                ActivityGridCell(
                    label = stringResource(R.string.activity_summary_distance),
                    segments = listOf(
                        MetricSegment(
                            "%.1f".format(distanceDisplay),
                            stringResource(state.distanceUnitRes),
                        ),
                    ),
                ),
                if (state.showActiveCalories) ActivityGridCell(
                    label = stringResource(R.string.activity_metric_calories),
                    segments = listOf(
                        MetricSegment(
                            "$calories",
                            stringResource(R.string.activity_unit_cal),
                        ),
                    ),
                ) else null,
                ActivityGridCell(
                    label = stringResource(R.string.activity_metric_duration),
                    segments = rememberDurationSegments(
                        totalSeconds = durationSeconds,
                        includeSeconds = true,
                    ),
                ),
            ),
        )
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    ActivityProgressCard(
        progressPercent = progressPercent,
        stepsProgress = if (state.stepsGoal > 0)
            (steps.toFloat() / state.stepsGoal).coerceIn(0f, 1f) else 0f,
        distanceProgress = if (distanceGoal > 0f)
            (distanceDisplay / distanceGoal).coerceIn(0f, 1f) else 0f,
        caloriesProgress = if (state.showActiveCalories && state.caloriesGoal > 0)
            (calories.toFloat() / state.caloriesGoal).coerceIn(0f, 1f) else 0f,
        showCalories = state.showActiveCalories,
        metrics = listOfNotNull(
            if (state.showActiveCalories) ActivityMetric(
                R.string.activity_metric_calories,
                "$calories",
                stringResource(R.string.activity_unit_cal),
            ) else null,
            ActivityMetric(
                R.string.activity_metric_distance,
                "%.1f".format(distanceDisplay),
                stringResource(state.distanceUnitRes),
            ),
            ActivityMetric(
                R.string.activity_metric_steps,
                "%,d".format(steps),
                stringResource(R.string.activity_unit_steps),
            ),
        ),
        onInfoClick = onInfoClick,
    )
}
