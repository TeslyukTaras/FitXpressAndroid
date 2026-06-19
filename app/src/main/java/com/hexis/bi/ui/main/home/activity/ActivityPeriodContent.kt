package com.hexis.bi.ui.main.home.activity

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.home.activity.components.ActivityAvgTrendRow
import com.hexis.bi.ui.main.home.activity.components.ActivityGridCell
import com.hexis.bi.ui.main.home.activity.components.ActivityMetricsGrid
import com.hexis.bi.ui.main.home.activity.components.MetricSegment
import com.hexis.bi.ui.main.home.activity.components.rememberDurationSegments

@Composable
fun ActivityPeriodContent(
    state: ActivityState,
    period: PeriodSummary,
    trendTitle: String,
    trendDescription: String,
    separateInsightGlass: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    belowNavigator: (@Composable () -> Unit)? = null,
    chart: @Composable () -> Unit,
) {

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    AppDateNavigator(
        label = period.periodLabel,
        onPrevious = onPrevious,
        onNext = onNext,
        canGoNext = period.canGoNext,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    if (belowNavigator != null) {
        belowNavigator()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }

    BodyGlassCard(
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_m),
            top = dimensionResource(R.dimen.spacer_l),
            end = dimensionResource(R.dimen.spacer_m),
            bottom = dimensionResource(R.dimen.spacer_l),
        ),
    ) {
        chart()

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        ActivityMetricsGrid(
            cells = listOfNotNull(
                ActivityGridCell(
                    label = stringResource(R.string.activity_summary_steps),
                    segments = listOf(
                        MetricSegment(
                            "%,d".format(period.totalSteps),
                            stringResource(R.string.activity_unit_steps_full),
                        ),
                    ),
                ),
                ActivityGridCell(
                    label = stringResource(R.string.activity_summary_distance),
                    segments = listOf(
                        MetricSegment(
                            "%.1f".format(period.totalDistanceKmDisplay(state.isMetric)),
                            stringResource(state.distanceUnitRes),
                        ),
                    ),
                ),
                if (state.showActiveCalories) ActivityGridCell(
                    label = stringResource(R.string.activity_metric_calories),
                    segments = listOf(
                        MetricSegment(
                            "%,d".format(period.totalCalories),
                            stringResource(R.string.activity_unit_cal),
                        ),
                    ),
                ) else null,
                ActivityGridCell(
                    label = stringResource(R.string.activity_metric_duration),
                    segments = rememberDurationSegments(
                        totalSeconds = period.totalActiveDurationSeconds,
                        includeSeconds = false,
                    ),
                ),
            ),
        )
    }

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    ActivityAvgTrendRow(
        avgStepsPerDay = period.avgStepsPerDay,
        trendPercent = period.trendPercent,
        trendComparison = period.trendComparison,
        trendTitle = trendTitle,
        trendDescription = trendDescription,
        separateInsightGlass = separateInsightGlass,
    )
}

private fun PeriodSummary.totalDistanceKmDisplay(isMetric: Boolean): Float =
    if (isMetric) totalDistanceKm else totalDistanceKm * com.hexis.bi.utils.constants.MeasurementConstants.KM_TO_MI
