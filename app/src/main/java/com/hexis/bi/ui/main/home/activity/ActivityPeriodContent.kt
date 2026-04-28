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
import com.hexis.bi.ui.main.home.activity.components.ActivityAvgTrendRow
import com.hexis.bi.ui.main.home.activity.components.ActivitySummaryCard
import com.hexis.bi.ui.main.home.activity.components.SummaryRow

@Composable
fun ActivityPeriodContent(
    state: ActivityState,
    period: PeriodSummary,
    totalsTitle: String,
    trendDescription: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    chart: @Composable () -> Unit,
) {
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = period.periodLabel,
        onPrevious = onPrevious,
        onNext = onNext,
        canGoNext = period.canGoNext,
    )

    chart()

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    ActivityAvgTrendRow(
        avgStepsPerDay = period.avgStepsPerDay,
        trendPercent = period.trendPercent,
        trendComparison = period.trendComparison,
        trendDescription = trendDescription,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

    ActivitySummaryCard(
        title = totalsTitle,
        rows = listOfNotNull(
            SummaryRow(
                label = stringResource(R.string.activity_summary_steps),
                value = "%,d".format(period.totalSteps),
                unit = stringResource(R.string.activity_unit_steps),
            ),
            SummaryRow(
                label = stringResource(R.string.activity_summary_distance),
                value = "%.1f".format(period.totalDistanceKmDisplay(state.isMetric)),
                unit = stringResource(state.distanceUnitRes),
            ),
            if (state.showActiveCalories) SummaryRow(
                label = stringResource(R.string.activity_summary_active_calories),
                value = "%,d".format(period.totalCalories),
                unit = stringResource(R.string.activity_unit_kcal),
            ) else null,
        ),
    )
}

private fun PeriodSummary.totalDistanceKmDisplay(isMetric: Boolean): Float =
    if (isMetric) totalDistanceKm else totalDistanceKm * com.hexis.bi.utils.constants.MeasurementConstants.KM_TO_MI
