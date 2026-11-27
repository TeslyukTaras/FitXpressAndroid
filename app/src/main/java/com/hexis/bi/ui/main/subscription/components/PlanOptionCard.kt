package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.PlanId
import com.hexis.bi.ui.components.AppRadioButton
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.subscription.nameRes
import com.hexis.bi.ui.theme.TitleDimTextStyle

internal enum class PlanBadge { MOST_POPULAR, CURRENT }

@Composable
internal fun PlanOptionCard(
    planId: PlanId,
    scansPerMonth: Int,
    price: String?,
    selected: Boolean,
    badge: PlanBadge?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        BodyGlassCard(
            loading = price == null,
            modifier = if (selected) Modifier.border(
                width = dimensionResource(R.dimen.border_hairline),
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large,
            ) else Modifier,
            shape = MaterialTheme.shapes.large,
            contentPadding = PaddingValues(
                vertical = dimensionResource(R.dimen.spacer_l),
                horizontal = dimensionResource(R.dimen.spacer_m)
            ),
            onClick = if (enabled) onClick else null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppRadioButton(selected = selected, onClick = null)
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xxs)),
                ) {
                    Text(
                        text = stringResource(planId.nameRes()),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.plan_scans_per_month, scansPerMonth),
                        style = TitleDimTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PlanPrice(price = price)
            }
        }
        if (badge != null) PlanBadgePill(
            text = stringResource(
                if (badge == PlanBadge.MOST_POPULAR) R.string.plan_badge_most_popular
                else R.string.plan_badge_current,
            ),
            highlighted = badge == PlanBadge.MOST_POPULAR,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = dimensionResource(R.dimen.spacer_l))
                .offsetUpByHalfHeight(),
        )
    }
}

