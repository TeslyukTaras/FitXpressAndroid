package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.components.EmptyStateContent
import com.hexis.bi.ui.main.subscription.components.EmptyStateMessage
import com.hexis.bi.ui.main.subscription.components.SubscriptionActionBar
import com.hexis.bi.ui.theme.screenBackground

@Composable
internal fun InsightsBlockedScreen(
    onBack: () -> Unit,
    onChoosePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIcons()

    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.subscription_title),
                background = Color.Transparent,
                titleStyle = MaterialTheme.typography.titleMedium,
                onBack = onBack,
            )
        },
        bottomBar = {
            SubscriptionActionBar {
                AppPrimaryButton(
                    text = stringResource(R.string.subscription_choose_plan),
                    onClick = onChoosePlan,
                )
            }
        },
    ) {
        EmptyStateContent(title = stringResource(R.string.insights_blocked_title)) {
            EmptyStateMessage(stringResource(R.string.insights_blocked_message))
        }
    }
}
