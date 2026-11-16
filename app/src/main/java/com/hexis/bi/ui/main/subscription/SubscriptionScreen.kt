package com.hexis.bi.ui.main.subscription

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.StorePlatform
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.domain.subscription.storePlatform
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.components.EmptyStateContent
import com.hexis.bi.ui.main.subscription.components.EmptyStateMessage
import com.hexis.bi.ui.main.subscription.components.InfoNote
import com.hexis.bi.ui.main.subscription.components.PlanDetailsSheet
import com.hexis.bi.ui.main.subscription.components.PlanSummaryCard
import com.hexis.bi.ui.main.subscription.components.SubscriptionActionBar
import com.hexis.bi.ui.main.subscription.components.RestoreResultDialog
import com.hexis.bi.ui.main.subscription.components.ScansUsage
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.AnimationConstants
import com.hexis.bi.utils.constants.SubscriptionConstants
import org.koin.androidx.compose.koinViewModel

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onChoosePlan: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val choosePlan = {
        val status = state.status
        if (status is SubscriptionStatus.Subscribed && status.platform == StorePlatform.IOS) {
            openManageSubscription(context, status)
        } else {
            onChoosePlan()
        }
    }

    LightStatusBarIcons()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.restore != null) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            viewModel = viewModel,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.subscription_title),
                    background = Color.Transparent,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    onBack = onBack,
                )
            },
            bottomBar = {
                SubscriptionBottomBar(
                    status = state.status,
                    onChoosePlan = choosePlan,
                    onRestore = viewModel::restorePurchases,
                    onResolvePayment = { openManageSubscription(context, state.status) },
                )
            },
        ) {
            AnimatedContent(
                targetState = state.status,
                contentKey = { it::class },
                transitionSpec = {
                    fadeIn(tween(AnimationConstants.CONTENT_SWAP_FADE_MS)) togetherWith
                            fadeOut(tween(AnimationConstants.CONTENT_SWAP_FADE_MS))
                },
                label = "subscriptionStatus",
            ) { status ->
                SubscriptionContent(
                    status = status,
                    state = state,
                    onViewDetails = viewModel::showPlanDetails,
                    onChangePlan = choosePlan,
                    onManage = { openManageSubscription(context, state.status) },
                )
            }
        }

        val planForDetails = when (val status = state.status) {
            is SubscriptionStatus.Subscribed -> status.plan
            is SubscriptionStatus.Expired -> status.plan
            is SubscriptionStatus.None, SubscriptionStatus.Loading -> null
        }
        if (state.showPlanDetails && planForDetails != null) {
            PlanDetailsSheet(
                plan = planForDetails,
                scansPerMonth = (state.status as? SubscriptionStatus.Subscribed)?.scansAllowed
                    ?: state.scansOf(planForDetails),
                onDismiss = viewModel::dismissPlanDetails,
            )
        }

        state.restore?.let { outcome ->
            RestoreResultDialog(
                outcome = outcome,
                onDismiss = viewModel::dismissRestore,
                onRetry = {
                    viewModel.dismissRestore()
                    viewModel.restorePurchases()
                },
            )
        }
    }
}

@Composable
internal fun SubscriptionContent(
    status: SubscriptionStatus,
    state: SubscriptionState,
    onViewDetails: () -> Unit,
    onChangePlan: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        SubscriptionStatus.Loading -> EmptyPlanState(loading = true, modifier = modifier)
        is SubscriptionStatus.None -> EmptyPlanState(loading = false, modifier = modifier)
        is SubscriptionStatus.Subscribed -> SubscribedContent(
            status = status,
            price = state.priceOf(status.plan),
            priceUnavailable = state.plansUnavailable,
            onViewDetails = onViewDetails,
            onChangePlan = onChangePlan,
            onManage = onManage,
            modifier = modifier,
        )

        is SubscriptionStatus.Expired -> ExpiredContent(
            status = status,
            price = state.priceOf(status.plan),
            priceUnavailable = state.plansUnavailable,
            onViewDetails = onViewDetails,
            onChangePlan = onChangePlan,
            onManage = onManage,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScrollColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = dimensionResource(R.dimen.padding_medium),
                top = dimensionResource(R.dimen.spacer_xxs),
                end = dimensionResource(R.dimen.padding_medium),
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xl)),
    ) {
        content()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
private fun SubscribedContent(
    status: SubscriptionStatus.Subscribed,
    price: String?,
    priceUnavailable: Boolean,
    onViewDetails: () -> Unit,
    onChangePlan: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollColumn(modifier) {
        PlanSummaryCard(
            label = stringResource(R.string.subscription_active_plan),
            name = stringResource(
                R.string.confirm_plan_name,
                stringResource(status.plan.nameRes())
            ),
            price = price,
            showPrice = status.platform == StorePlatform.ANDROID && !priceUnavailable,
        )
        BodyGlassCard(contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l))) {
            Text(
                text = stringResource(R.string.subscription_scans_this_month),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            ScansUsage(status = status, barGap = dimensionResource(R.dimen.spacer_xs))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            InfoNote(text = stringResource(R.string.subscription_no_rollover))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            AppHorizontalGradientDivider()
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
                Text(
                    text = stringResource(
                        if (status is SubscriptionStatus.Active && !status.renews) {
                            R.string.subscription_ends_on
                        } else {
                            R.string.subscription_renews_on
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = dimensionResource(R.dimen.spacer_xs)),
                )
                Text(
                    text = status.renewsOn.formatDisplay(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        BodyGlassCard(contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_xs))) {
            if (status is SubscriptionStatus.GracePeriod)
                PaymentIssueHeader(graceEndsOn = status.graceEndsOn.formatDisplay())
            LinkRows(
                onViewDetails = onViewDetails,
                onChangePlan = onChangePlan,
                onManage = onManage,
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
private fun PaymentIssueHeader(graceEndsOn: String) {
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.spacer_xs))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = NocturnePulseTheme.extendedColors.warning,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
            Text(
                text = stringResource(R.string.subscription_payment_issue),
                style = MaterialTheme.typography.bodyLarge,
                color = NocturnePulseTheme.extendedColors.warning,
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacer_xs)),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.subscription_grace_message, graceEndsOn),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        AppHorizontalGradientDivider()
    }
}

@Composable
private fun ExpiredContent(
    status: SubscriptionStatus.Expired,
    price: String?,
    priceUnavailable: Boolean,
    onViewDetails: () -> Unit,
    onChangePlan: () -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrollColumn(modifier) {
        PlanSummaryCard(
            label = stringResource(
                if (status.paymentIssue) R.string.subscription_payment_issue else R.string.subscription_inactive_label,
            ),
            labelColor = if (status.paymentIssue) {
                NocturnePulseTheme.extendedColors.warning
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            name = stringResource(
                R.string.confirm_plan_name,
                stringResource(status.plan.nameRes())
            ),
            price = price,
            showPrice = status.platform == StorePlatform.ANDROID && !priceUnavailable,
        )
        BodyGlassCard(contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l))) {
            InfoNote(
                text = stringResource(
                    if (status.paymentIssue) R.string.subscription_on_hold_message else R.string.subscription_expired_message,
                ),
                icon = R.drawable.ic_calendar,
                textStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                gap = dimensionResource(R.dimen.spacer_xs),
            )
        }
        BodyGlassCard(contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_xs))) {
            LinkRows(
                onViewDetails = onViewDetails,
                onChangePlan = onChangePlan,
                onManage = onManage,
                showChangePlan = !status.paymentIssue,
            )
        }
    }
}

@Composable
private fun LinkRows(
    onViewDetails: () -> Unit,
    onChangePlan: () -> Unit,
    onManage: () -> Unit,
    showChangePlan: Boolean = true,
) {
    LinkRow(R.string.subscription_view_details, onViewDetails)
    if (showChangePlan) LinkRow(R.string.subscription_change_plan, onChangePlan)
    LinkRow(R.string.subscription_manage, onManage)
}

@Composable
private fun LinkRow(@StringRes label: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(dimensionResource(R.dimen.spacer_xs)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}

@Composable
private fun EmptyPlanState(loading: Boolean, modifier: Modifier = Modifier) {
    EmptyStateContent(
        title = stringResource(R.string.subscription_none_title),
        modifier = modifier,
        hidden = loading,
    ) {
        EmptyStateMessage(stringResource(R.string.subscription_none_message))
    }
}

@Composable
internal fun SubscriptionBottomBar(
    status: SubscriptionStatus,
    onChoosePlan: () -> Unit,
    onRestore: () -> Unit,
    onResolvePayment: () -> Unit,
) {
    val hasActions = status !is SubscriptionStatus.Active && status !is SubscriptionStatus.Loading
    if (!hasActions) return
    SubscriptionActionBar {
        when (status) {
            is SubscriptionStatus.GracePeriod -> AppPrimaryButton(
                text = stringResource(R.string.subscription_resolve_payment),
                onClick = onResolvePayment,
            )

            is SubscriptionStatus.Expired -> if (status.paymentIssue) {
                AppPrimaryButton(
                    text = stringResource(R.string.subscription_resolve_payment),
                    onClick = onResolvePayment,
                )
            } else {
                AppPrimaryButton(
                    text = stringResource(R.string.subscription_renew),
                    onClick = onChoosePlan,
                )
                AppOutlinedButton(
                    text = stringResource(R.string.plans_restore),
                    onClick = onRestore,
                )
            }

            is SubscriptionStatus.None -> {
                AppPrimaryButton(
                    text = stringResource(R.string.subscription_choose_plan),
                    onClick = onChoosePlan,
                )
                AppOutlinedButton(
                    text = stringResource(R.string.plans_restore),
                    onClick = onRestore,
                )
            }

            is SubscriptionStatus.Active, SubscriptionStatus.Loading -> Unit
        }
    }
}

internal fun openManageSubscription(context: Context, status: SubscriptionStatus) {
    openManageSubscription(context, status.storePlatform ?: StorePlatform.ANDROID)
}

internal fun openManageSubscription(context: Context, platform: StorePlatform) {
    val uri = if (platform == StorePlatform.IOS) {
        SubscriptionConstants.APPLE_SUBSCRIPTIONS_URL.toUri()
    } else {
        SubscriptionConstants.PLAY_SUBSCRIPTIONS_URL.toUri().buildUpon()
            .appendQueryParameter(SubscriptionConstants.PLAY_SUBSCRIPTIONS_PACKAGE_PARAM, context.packageName)
            .build()
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
    }
}
