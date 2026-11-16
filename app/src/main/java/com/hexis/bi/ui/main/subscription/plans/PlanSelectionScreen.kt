package com.hexis.bi.ui.main.subscription.plans

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.theme.screenBackground

@Composable
fun PlanSelectionScreen(
    onBack: () -> Unit,
    onClose: () -> Unit,
    onContinue: (PlanId) -> Unit,
    onRestored: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.subscription_title),
                background = Color.Transparent,
                onBack = onBack,
            )
        },
    ) {}
}
