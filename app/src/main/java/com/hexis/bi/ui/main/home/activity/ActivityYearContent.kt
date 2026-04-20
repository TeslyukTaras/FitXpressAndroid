package com.hexis.bi.ui.main.home.activity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.components.ActivityScrollableStepsBarChart
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityYearContent(
    state: ActivityState,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
) {
    val trendDescription = when (state.year.trendComparison) {
        TrendComparison.UP -> stringResource(R.string.activity_trend_up_year)
        TrendComparison.DOWN -> stringResource(R.string.activity_trend_down_year)
        TrendComparison.FLAT -> stringResource(R.string.activity_trend_flat_year)
        TrendComparison.NONE -> stringResource(R.string.activity_trend_none_year)
    }

    ActivityPeriodContent(
        state = state,
        period = state.year,
        totalsTitle = stringResource(R.string.activity_yearly_totals),
        trendDescription = trendDescription,
        onPrevious = onPreviousYear,
        onNext = onNextYear,
    ) {
        ActivityScrollableStepsBarChart(
            entries = state.year.bars,
            totalValue = state.year.totalSteps,
            baseYMax = ActivityConstants.YEAR_STEP_GRID_MAX,
            yGridStep = ActivityConstants.YEAR_STEP_GRID_STEP,
            title = stringResource(R.string.activity_total_steps),
            barWidth = dimensionResource(R.dimen.activity_year_bar_width),
            barGap = dimensionResource(R.dimen.spacer_s),
            yAxisWidth = dimensionResource(R.dimen.activity_year_y_axis_width),
            yLabelFormatter = ::formatShortThousand,
            isLastHighlighted = !state.year.canGoNext,
            scrollAlignIndex = state.year.bars.indexOfLast { it.value > 0f }
                .takeIf { it >= 0 } ?: state.year.bars.lastIndex,
        )
    }
}

private fun formatShortThousand(value: Float): String =
    if (value >= 1_000f) "${(value / 1_000f).toInt()}k" else value.toInt().toString()
