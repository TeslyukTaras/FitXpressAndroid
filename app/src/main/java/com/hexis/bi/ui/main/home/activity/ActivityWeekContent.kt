package com.hexis.bi.ui.main.home.activity

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.main.home.activity.components.ActivityStepsBarChart
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityWeekContent(
    state: ActivityState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state.weekLoadState) {
        ActivityLoadState.Loading -> ActivityLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        ActivityLoadState.Error -> ActivityLoadPlaceholder {
            Text(
                text = stringResource(R.string.activity_error_title),
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

        ActivityLoadState.Ready -> ActivityWeekReady(
            state = state,
            onPreviousWeek = onPreviousWeek,
            onNextWeek = onNextWeek,
        )
    }
}

@Composable
private fun ActivityWeekReady(
    state: ActivityState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val trendDescription = when (state.week.trendComparison) {
        TrendComparison.UP -> stringResource(R.string.activity_trend_up_week)
        TrendComparison.DOWN -> stringResource(R.string.activity_trend_down_week)
        TrendComparison.FLAT -> stringResource(R.string.activity_trend_flat_week)
        TrendComparison.NONE -> stringResource(R.string.activity_trend_none_week)
    }

    ActivityPeriodContent(
        state = state,
        period = state.week,
        totalsTitle = stringResource(R.string.activity_weekly_totals),
        trendDescription = trendDescription,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
    ) {
        ActivityStepsBarChart(
            entries = state.week.bars,
            totalValue = state.week.totalSteps,
            baseYMax = ActivityConstants.PERIOD_STEP_GRID_MAX,
            yGridStep = ActivityConstants.PERIOD_STEP_GRID_STEP,
            title = stringResource(R.string.activity_total_steps),
            barGap = dimensionResource(R.dimen.spacer_s),
            yAxisWidth = dimensionResource(R.dimen.activity_period_y_axis_width),
            xLabelStartPadding = dimensionResource(R.dimen.spacer_xxs),
            isLastHighlighted = !state.week.canGoNext,
        )
    }
}
