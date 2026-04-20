package com.hexis.bi.ui.main.home.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.components.ActivityStepsBarChart
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityMonthContent(
    state: ActivityState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val trendDescription = when (state.month.trendComparison) {
        TrendComparison.UP -> stringResource(R.string.activity_trend_up_month)
        TrendComparison.DOWN -> stringResource(R.string.activity_trend_down_month)
        TrendComparison.FLAT -> stringResource(R.string.activity_trend_flat_month)
        TrendComparison.NONE -> stringResource(R.string.activity_trend_none_month)
    }

    ActivityPeriodContent(
        state = state,
        period = state.month,
        totalsTitle = stringResource(R.string.activity_period_totals),
        trendDescription = trendDescription,
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
    ) {
        ActivityStepsBarChart(
            entries = state.month.bars,
            totalValue = state.month.totalSteps,
            baseYMax = ActivityConstants.PERIOD_STEP_GRID_MAX,
            yGridStep = ActivityConstants.PERIOD_STEP_GRID_STEP,
            title = stringResource(R.string.activity_total_steps),
            barGap = dimensionResource(R.dimen.spacer_xxs),
            yAxisWidth = dimensionResource(R.dimen.activity_period_y_axis_width),
        )
    }
}
