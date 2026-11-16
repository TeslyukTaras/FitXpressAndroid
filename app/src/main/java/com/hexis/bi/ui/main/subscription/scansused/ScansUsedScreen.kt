package com.hexis.bi.ui.main.subscription.scansused

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.SubscriptionViewModel
import com.hexis.bi.ui.main.subscription.formatDisplay
import com.hexis.bi.ui.main.subscription.nameRes
import com.hexis.bi.ui.main.subscription.openManageSubscription
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.SubscriptionConstants
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
                    text = stringResource(R.string.scans_used_upgrade),
                    onClick = {
                        if (status?.platform == StorePlatform.IOS) openManageSubscription(context, status)
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
    val planLine = emphasized(
        text = stringResource(
            R.string.scans_used_plan,
            stringResource(status.plan.nameRes()),
            status.scansAllowed,
        ),
        parts = listOf(status.scansAllowed.toString()),
    )
    val resetLine = emphasized(
        text = stringResource(R.string.scans_used_reset, status.renewsOn.formatDisplay()),
        parts = listOf(status.renewsOn.formatDisplay()),
    )
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
                text = stringResource(R.string.scans_used_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = planLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Text(
                text = resetLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
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
