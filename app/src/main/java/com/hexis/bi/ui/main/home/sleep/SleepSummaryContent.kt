package com.hexis.bi.ui.main.home.sleep

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
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.sleep.components.SleepRecoveryBanner
import com.hexis.bi.ui.main.home.sleep.components.SleepStructureCard

@Composable
fun SleepSummaryContent(
    state: SleepState,
    onInfoClick: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRetry: () -> Unit = {},
) {

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
    AppDateNavigator(
        modifier = Modifier,
        label = state.weekLabel,
        onPrevious = onPreviousWeek,
        onNext = onNextWeek,
        canGoNext = state.canGoNextWeek,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

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
    SleepStructureCard(
        structure = state.weeklyStructure,
        stages = state.weeklyStages,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    SleepRecoveryBanner(onInfoClick = onInfoClick)
}
