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
fun ActivityMonthContent(
    state: ActivityState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state.monthLoadState) {
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

        ActivityLoadState.Ready -> ActivityMonthReady(
            state = state,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
        )
    }
}

@Composable
private fun ActivityMonthReady(
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
        trendTitle = stringResource(R.string.activity_trend_label_month),
        trendDescription = trendDescription,
        separateInsightGlass = false,
        onPrevious = onPreviousMonth,
        onNext = onNextMonth,
    ) {
        ActivityStepsBarChart(
            entries = state.month.bars,
            totalValue = state.month.totalSteps,
            baseYMax = ActivityConstants.PERIOD_STEP_GRID_MAX,
            yGridStep = ActivityConstants.PERIOD_STEP_GRID_STEP,
            title = stringResource(R.string.activity_total_steps),
            barGap = dimensionResource(R.dimen.activity_chart_bar_gap_compact),
            chartStartPadding = dimensionResource(R.dimen.activity_chart_start_padding_compact),
            chartEndPadding = dimensionResource(R.dimen.activity_chart_end_padding),
            showVerticalGridLines = true,
        )
    }
}
