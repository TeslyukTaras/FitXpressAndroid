package com.hexis.bi.ui.main.home.sleep

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.sleep.components.SleepBarChartCard
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStagesWeeklyCard

@Composable
fun SleepSummaryContent(
    modifier: Modifier = Modifier,
    state: SleepState,
    onInfoClick: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRetry: () -> Unit = {},
) {
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = state.weekLabel,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        canGoNext = state.canGoNextWeek,
    )

    when (state.summaryLoadState) {
        SleepLoadState.Loading -> SleepLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        SleepLoadState.Error -> SleepLoadPlaceholder {
            Text(
                text = stringResource(R.string.sleep_error_title),
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

        SleepLoadState.Ready -> SleepSummaryReady(state = state, onInfoClick = onInfoClick)
    }
}

@Composable
private fun SleepSummaryReady(
    state: SleepState,
    onInfoClick: () -> Unit,
) {
    SleepBarChartCard(entries = state.weeklyEntries)

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

    SleepStagesWeeklyCard(
        stages = state.weeklyStages,
        avgSleepMinutes = state.avgSleepMinutes,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    SleepRecoveryBanner(onInfoClick = onInfoClick)
}
