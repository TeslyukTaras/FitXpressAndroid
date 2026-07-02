package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.recovery.components.RecoveryHrvCard
import com.hexis.bi.ui.main.home.recovery.components.RecoveryInfoCard
import com.hexis.bi.ui.main.home.recovery.components.RecoveryStatusCard

@Composable
fun RecoveryDayContent(
    state: RecoveryState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
    AppDateNavigator(
        modifier = Modifier,
        label = state.dateLabel,
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        canGoNext = state.canGoNextDay,
    )
    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

    when (state.dayLoadState) {
        RecoveryLoadState.Loading -> RecoveryLoadPlaceholder {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        // No "cannot load" view: a failed/empty load falls through to the cards,
        // which render "—" for any missing values.
        RecoveryLoadState.Ready,
        RecoveryLoadState.Error -> RecoveryDayReady(state = state, onInfoClick = onInfoClick)
    }
}

@Composable
private fun RecoveryDayReady(state: RecoveryState, onInfoClick: () -> Unit) {
    RecoveryStatusCard(
        score = state.score,
        status = state.status,
        metrics = state.metrics,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    RecoveryHrvCard(
        rmssdMs = state.rmssdMs,
        sdnnMs = state.sdnnMs,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

    RecoveryInfoCard(onInfoClick = onInfoClick)
}

@Composable
fun RecoveryLoadPlaceholder(content: @Composable () -> Unit) {
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
