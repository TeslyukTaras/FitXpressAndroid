package com.hexis.bi.ui.main.subscription.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.domain.subscription.SubscriptionPlan
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.main.subscription.components.CloseTopBar
import com.hexis.bi.ui.main.subscription.components.EqualWidthRow
import com.hexis.bi.ui.main.subscription.components.FeatureTile
import com.hexis.bi.ui.main.subscription.components.FooterLinkItem
import com.hexis.bi.ui.main.subscription.components.FooterLinks
import com.hexis.bi.ui.main.subscription.components.InfoNote
import com.hexis.bi.ui.main.subscription.components.PlanBadge
import com.hexis.bi.ui.main.subscription.components.PlanOptionCard
import com.hexis.bi.ui.main.subscription.nameRes
import com.hexis.bi.ui.theme.NocturnePulseTheme

internal val MostPopularPlan = PlanId.PROGRESS

@Composable
internal fun PlanSelectionTopBar(
    isChangeMode: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    if (isChangeMode) {
        BaseTopBar(
            title = stringResource(R.string.plans_change_title),
            background = Color.Transparent,
            titleStyle = MaterialTheme.typography.titleMedium,
            onBack = onBack,
        )
    } else {
        CloseTopBar(onClose = onClose)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanSelectionBody(
    plans: List<SubscriptionPlan>?,
    selected: PlanId?,
    currentPlan: PlanId?,
    onSelect: (PlanId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isChangeMode = currentPlan != null
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
    ) {
        if (isChangeMode) {
            val appBarTitleBottomInset = (
                    TopAppBarDefaults.TopAppBarExpandedHeight -
                            with(LocalDensity.current) {
                                MaterialTheme.typography.titleMedium.lineHeight.toDp()
                            }
                    ) / 2
            Spacer(
                Modifier.height(
                    (dimensionResource(R.dimen.subscription_change_title_gap) - appBarTitleBottomInset)
                        .coerceAtLeast(0.dp),
                ),
            )
        }
        Text(
            text = stringResource(if (isChangeMode) R.string.plans_change_heading else R.string.plans_title),
            style = if (isChangeMode) MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)
            else MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
            color = NocturnePulseTheme.extendedColors.textEmphasis,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        Text(
            text = stringResource(R.string.plans_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        EqualWidthRow(gap = dimensionResource(R.dimen.spacer_xl)) {
            FeatureTile(R.drawable.ic_scan, R.string.plans_feature_scan)
            FeatureTile(R.drawable.ic_chart_bar, R.string.plans_feature_trends)
            FeatureTile(R.drawable.ic_sparkle, R.string.plans_feature_insights)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_l)),
        ) {
            PlanId.entries.forEach { planId ->
                val plan = plans?.firstOrNull { it.id == planId }
                val price = plan?.formattedPrice
                PlanOptionCard(
                    planId = planId,
                    scansPerMonth = plan?.scansPerMonth ?: planId.scansPerMonth,
                    price = price,
                    selected = planId == selected,
                    badge = when {
                        isChangeMode -> if (planId == currentPlan) PlanBadge.CURRENT else null
                        (plan?.mostPopular ?: (planId == MostPopularPlan)) -> PlanBadge.MOST_POPULAR
                        else -> null
                    },
                    enabled = price != null && planId != currentPlan,
                    onClick = { onSelect(planId) },
                )
            }
        }

        if (!isChangeMode) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            InfoNote(text = stringResource(R.string.plans_auto_renew))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
internal fun PlanSelectionBottomBar(
    selected: PlanId?,
    isChangeMode: Boolean,
    onContinue: () -> Unit,
    onRestore: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .padding(
                top = dimensionResource(R.dimen.spacer_m),
                bottom = dimensionResource(R.dimen.spacer_xs),
            )
            .navigationBarsPadding(),
    ) {
        AppPrimaryButton(
            text = when {
                isChangeMode -> stringResource(R.string.plans_review_change)
                selected != null -> stringResource(
                    R.string.plans_continue_with,
                    stringResource(selected.nameRes()),
                )

                else -> stringResource(R.string.plans_continue_with_plan)
            },
            onClick = onContinue,
            enabled = selected != null,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
        if (isChangeMode) Text(
            text = stringResource(R.string.plans_cancel_anytime),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacer_s)),
        )
        else FooterLinks(
            links = listOf(
                FooterLinkItem(stringResource(R.string.plans_restore), onRestore),
                FooterLinkItem(stringResource(R.string.plans_terms), onOpenTerms),
                FooterLinkItem(stringResource(R.string.plans_privacy), onOpenPrivacy),
            ),
        )
    }
}
