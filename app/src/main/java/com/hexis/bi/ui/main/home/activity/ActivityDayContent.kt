package com.hexis.bi.ui.main.home.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.activity.components.ActivityDayDetail

@Composable
fun ActivityDayContent(
    state: ActivityState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state.dayLoadState) {
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

        ActivityLoadState.Ready -> ActivityDayReady(
            state = state,
            onInfoClick = onInfoClick,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
        )
    }
}

@Composable
private fun ActivityDayReady(
    state: ActivityState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

    AppDateNavigator(
        label = state.dateLabel,
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        canGoNext = state.canGoNextDay,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    ActivityDayDetail(
        state = state,
        steps = state.currentSteps,
        distanceKm = state.distanceKm,
        calories = state.calories,
        durationSeconds = state.activeDurationSeconds,
        hourlyBars = state.hourlyBars,
        onInfoClick = onInfoClick,
    )
}

@Composable
fun ActivityLoadPlaceholder(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimensionResource(R.dimen.spacer_3xl)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}
