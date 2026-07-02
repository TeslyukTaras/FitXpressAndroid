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
import com.hexis.bi.ui.main.home.activity.components.ActivityDayDetail
import com.hexis.bi.ui.main.home.activity.components.ActivitySelectedDayHeader
import com.hexis.bi.ui.main.home.activity.components.ActivityStepsBarChart
import com.hexis.bi.ui.main.home.activity.components.ActivityWeekDayCircles
import com.hexis.bi.utils.constants.ActivityConstants

@Composable
fun ActivityWeekContent(
    state: ActivityState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectWeekDay: (Int) -> Unit,
    onClearWeekDay: () -> Unit,
    onInfoClick: () -> Unit,
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
            onSelectWeekDay = onSelectWeekDay,
            onClearWeekDay = onClearWeekDay,
            onInfoClick = onInfoClick,
        )
    }
}

@Composable
private fun ActivityWeekReady(
    state: ActivityState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSelectWeekDay: (Int) -> Unit,
    onClearWeekDay: () -> Unit,
    onInfoClick: () -> Unit,
) {
    val selectedDay = state.selectedWeekDay
    if (selectedDay != null) {
        ActivityWeekSelectedDay(
            state = state,
            selectedDay = selectedDay,
            onSelectWeekDay = onSelectWeekDay,
            onClearWeekDay = onClearWeekDay,
            onInfoClick = onInfoClick,
        )
        return
    }

    val trendDescription = when (state.week.trendComparison) {
        TrendComparison.NONE -> stringResource(R.string.activity_trend_none_week)
        else -> stringResource(R.string.activity_trend_insight_week)
    }

    ActivityPeriodContent(
        state = state,
        period = state.week,
        trendTitle = stringResource(R.string.activity_trend_label_week),
        trendDescription = trendDescription,
        separateInsightGlass = true,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        belowNavigator = {
            ActivityWeekDayCircles(
                days = state.weekDays,
                stepsGoal = state.stepsGoal,
                selectedIndex = state.selectedWeekDayIndex,
                onSelect = onSelectWeekDay,
            )
        },
    ) {
        ActivityStepsBarChart(
            entries = state.week.bars,
            totalValue = state.week.totalSteps,
            baseYMax = ActivityConstants.PERIOD_STEP_GRID_MAX,
            yGridStep = ActivityConstants.PERIOD_STEP_GRID_STEP,
            title = stringResource(R.string.activity_total_steps),
            barGap = dimensionResource(R.dimen.activity_chart_bar_gap_expanded),
            chartStartPadding = dimensionResource(R.dimen.activity_chart_start_padding_expanded),
            chartEndPadding = dimensionResource(R.dimen.activity_chart_end_padding),
            xLabelStartPadding = dimensionResource(R.dimen.spacer_xxs),
            highlightedXLabelIndex = state.weekDays.indexOfFirst { it.isToday },
        )
    }
}

@Composable
private fun ActivityWeekSelectedDay(
    state: ActivityState,
    selectedDay: WeekDayData,
    onSelectWeekDay: (Int) -> Unit,
    onClearWeekDay: () -> Unit,
    onInfoClick: () -> Unit,
) {

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    ActivitySelectedDayHeader(
        label = selectedDay.selectedDateLabel,
        onClear = onClearWeekDay,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    ActivityWeekDayCircles(
        days = state.weekDays,
        stepsGoal = state.stepsGoal,
        selectedIndex = state.selectedWeekDayIndex,
        onSelect = onSelectWeekDay,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    ActivityDayDetail(
        state = state,
        steps = selectedDay.steps,
        distanceKm = selectedDay.distanceKm,
        calories = selectedDay.calories,
        durationSeconds = selectedDay.durationSeconds,
        hourlyBars = selectedDay.hourlyBars,
        onInfoClick = onInfoClick,
    )
}
