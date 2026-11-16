package com.hexis.bi.ui.main.home.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.openManageSubscription
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.SubscriptionConstants

@Composable
internal fun InsightsBlockedScreen(
    paymentIssue: Boolean,
    storePlatform: StorePlatform,
    onBack: () -> Unit,
    onChoosePlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                    .padding(
                        top = dimensionResource(R.dimen.spacer_m),
                        bottom = dimensionResource(R.dimen.spacer_xl),
                    )
                    .navigationBarsPadding(),
            ) {
                AppPrimaryButton(
                    text = stringResource(
                        if (paymentIssue) R.string.subscription_resolve_payment else R.string.subscription_choose_plan,
                    ),
                    onClick = {
                        if (paymentIssue) openManageSubscription(context, storePlatform) else onChoosePlan()
                    },
                )
            }
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.subscription_empty_icon_tile))
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = SubscriptionConstants.EMPTY_TILE_ALPHA),
                            shape = MaterialTheme.shapes.medium,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_layers),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimensionResource(R.dimen.subscription_empty_icon_size)),
                    )
                }
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                Text(
                    text = stringResource(R.string.insights_blocked_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
                Text(
                    text = stringResource(R.string.insights_blocked_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
