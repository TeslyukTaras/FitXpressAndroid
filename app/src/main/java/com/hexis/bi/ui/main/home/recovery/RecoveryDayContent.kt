package com.hexis.bi.ui.main.home.recovery

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppDateNavigator
import com.hexis.bi.ui.main.home.recovery.components.RecoveryInfoCard
import com.hexis.bi.ui.main.home.recovery.components.RecoveryStatusCard

@Composable
fun RecoveryDayContent(
    state: RecoveryState,
    onInfoClick: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
) {
    AppDateNavigator(
        modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacer_xxs)),
        label = state.dateLabel,
        onPrevious = onPreviousDay,
        onNext = onNextDay,
        canGoNext = state.canGoNextDay,
    )

    RecoveryStatusCard(
        score = state.score,
        status = state.status,
        metrics = state.metrics,
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    RecoveryInfoCard(onInfoClick = onInfoClick)
}
