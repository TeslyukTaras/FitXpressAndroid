package com.hexis.bi.ui.main.subscription.scansused

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.domain.subscription.isTopPlan
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.SubscriptionViewModel
import com.hexis.bi.ui.main.subscription.components.EmptyStateContent
import com.hexis.bi.ui.main.subscription.components.EmptyStateMessage
import com.hexis.bi.ui.main.subscription.components.SubscriptionActionBar
import com.hexis.bi.ui.main.subscription.formatDisplay
import com.hexis.bi.ui.main.subscription.nameRes
import com.hexis.bi.ui.main.subscription.openManageSubscription
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import org.koin.androidx.compose.koinViewModel

@Composable
fun ScansUsedScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status = state.status as? SubscriptionStatus.Subscribed
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
            if (status != null && !status.plan.isTopPlan) SubscriptionActionBar {
                AppPrimaryButton(
                    text = stringResource(R.string.scans_used_upgrade),
                    onClick = {
                        if (status.platform == StorePlatform.IOS) openManageSubscription(
                            context,
                            status
                        )
                        else onUpgrade()
                    },
                )
            }
        },
    ) {
        if (status != null) ScansUsedContent(status)
    }
}

@Composable
private fun ScansUsedContent(status: SubscriptionStatus.Subscribed) {
    val allowance = stringResource(R.string.plan_scans_per_month, status.scansAllowed)
    val planLine = emphasized(
        text = stringResource(R.string.scans_used_plan, stringResource(status.plan.nameRes()), allowance),
        parts = listOf(allowance),
    )
    val resetLine = emphasized(
        text = stringResource(R.string.scans_used_reset, status.renewsOn.formatDisplay()),
        parts = listOf(status.renewsOn.formatDisplay()),
    )
    EmptyStateContent(title = stringResource(R.string.scans_used_title)) {
        EmptyStateMessage(planLine)
        EmptyStateMessage(resetLine)
    }
}

@Composable
private fun emphasized(text: String, parts: List<String>): AnnotatedString {
    val emphasis = SpanStyle(
        fontWeight = FontWeight.Medium,
        color = NocturnePulseTheme.extendedColors.textEmphasis,
    )
    return buildAnnotatedString {
        append(text)
        parts.forEach { part ->
            val start = text.indexOf(part)
            if (start >= 0) addStyle(emphasis, start, start + part.length)
        }
    }
}
