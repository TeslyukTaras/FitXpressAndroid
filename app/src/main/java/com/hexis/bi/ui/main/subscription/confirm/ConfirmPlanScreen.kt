package com.hexis.bi.ui.main.subscription.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.domain.subscription.scansRemaining
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.base.UiEvent
import com.hexis.bi.ui.components.AppHorizontalGradientDivider
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.subscription.components.InfoNote
import com.hexis.bi.ui.main.subscription.components.PlanBadgePill
import com.hexis.bi.ui.main.subscription.components.PlanFeatureList
import com.hexis.bi.ui.main.subscription.components.PlanPrice
import com.hexis.bi.ui.main.subscription.components.PlanSummaryCard
import com.hexis.bi.ui.main.subscription.components.TopEndBadgeContainer
import com.hexis.bi.ui.main.subscription.formatDisplay
import com.hexis.bi.ui.main.subscription.nameRes
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.ui.theme.TitleDimTextStyle
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ConfirmPlanScreen(
    planId: PlanId,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfirmPlanViewModel = koinViewModel { parametersOf(planId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isChange = state.kind != PlanChangeKind.SUBSCRIBE
    val planName = stringResource(planId.nameRes())

    LightStatusBarIcons()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ConfirmPlanEvent.Completed -> onCompleted()
                    is UiEvent.NavigateBack -> onBack()
                    else -> Unit
                }
            }
        }
    }

    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .screenBackground(),
        containerColor = Color.Transparent,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::dismissError,
        viewModel = viewModel,
        topBar = {
            BaseTopBar(
                title = stringResource(
                    if (isChange) R.string.confirm_change_title else R.string.confirm_plan_title,
                ),
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
                    text = if (isChange) stringResource(R.string.confirm_switch, planName)
                    else stringResource(
                        R.string.confirm_subscribe,
                        state.plan?.formattedPrice.orEmpty()
                    ),
                    onClick = viewModel::confirm,
                    enabled = state.plan != null && state.status !is SubscriptionStatus.Loading && !isLoading,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
                Text(
                    text = stringResource(
                        if (isChange) R.string.confirm_footer_change else R.string.confirm_footer_subscribe,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        ConfirmPlanContent(state = state)
    }
}

@Composable
internal fun ConfirmPlanContent(state: ConfirmPlanState) {
    val current = state.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        if (current == null) Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxs)))

        if (current != null) {
            CurrentPlanRow(
                planId = current.plan,
                scansPerMonth = current.scansAllowed,
                price = state.currentPrice,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
            Text(
                text = stringResource(R.string.confirm_change_to),
                style = MaterialTheme.typography.bodyLarge,
                color = NocturnePulseTheme.extendedColors.textEmphasis,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }

        PlanSummaryCard(
            name = if (current != null) stringResource(state.planId.nameRes())
            else stringResource(R.string.confirm_plan_name, stringResource(state.planId.nameRes())),
            price = state.plan?.formattedPrice,
            scansText = stringResource(R.string.plan_scans_per_month, state.scansPerMonth),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        Text(
            text = stringResource(R.string.confirm_what_you_get),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        PlanFeatureList(plan = state.planId, scansPerMonth = state.scansPerMonth)

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        AppHorizontalGradientDivider()
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        if (current == null) InfoNote(text = stringResource(R.string.confirm_scans_note))
        else {
            InfoNote(
                text = if (state.kind == PlanChangeKind.UPGRADE) {
                    stringResource(
                        R.string.confirm_upgrade_scans_note,
                        (state.scansPerMonth - current.scansUsed).coerceAtLeast(0),
                        state.scansPerMonth,
                    )
                } else {
                    pluralStringResource(
                        R.plurals.confirm_remaining_scans_note,
                        current.scansRemaining,
                        current.scansRemaining,
                    )
                },
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            val date = current.renewsOn.formatDisplay()
            InfoNote(
                icon = R.drawable.ic_calendar,
                text = when (state.kind) {
                    PlanChangeKind.DOWNGRADE -> stringResource(
                        R.string.confirm_downgrade_note,
                        date,
                        stringResource(state.planId.nameRes()),
                    )

                    else -> stringResource(R.string.confirm_upgrade_note, date)
                },
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
private fun CurrentPlanRow(planId: PlanId, scansPerMonth: Int, price: String?) {
    TopEndBadgeContainer(
        badge = {
            PlanBadgePill(
                text = stringResource(R.string.plan_badge_current),
                highlighted = false,
            )
        },
        badgeEndPadding = dimensionResource(R.dimen.spacer_xl),
    ) {
        BodyGlassCard(
            loading = price == null,
            contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.spacer_m),
                vertical = dimensionResource(R.dimen.spacer_l)
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(planId.nameRes()),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                        color = NocturnePulseTheme.extendedColors.textEmphasis,
                    )
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                    Text(
                        text = stringResource(R.string.plan_scans_per_month, scansPerMonth),
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                PlanPrice(price = price, gap = dimensionResource(R.dimen.spacer_3xs))
            }
        }
    }
}
