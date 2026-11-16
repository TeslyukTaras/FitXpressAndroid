package com.hexis.bi.ui.main.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.main.subscription.components.InfoNote
import com.hexis.bi.ui.main.subscription.components.ScansUsage
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun SettingsPlanCard(
    status: SubscriptionStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_l)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_layers),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
            Text(
                text = stringResource(R.string.settings_your_plan),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(R.dimen.spacer_m)),
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
        PlanCardDetails(status)
    }
}

@Composable
private fun PlanCardDetails(status: SubscriptionStatus) {
    when (status) {
        SubscriptionStatus.Loading -> StatusLine(
            text = stringResource(R.string.subscription_none_title),
            modifier = Modifier.alpha(0f),
        )

        SubscriptionStatus.None -> StatusLine(stringResource(R.string.subscription_none_title))
        is SubscriptionStatus.Expired -> StatusLine(stringResource(R.string.subscription_inactive_label))
        is SubscriptionStatus.Subscribed -> Column {
            ScansUsage(status = status, barGap = dimensionResource(R.dimen.spacer_2xs))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
            if (status is SubscriptionStatus.GracePeriod) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning),
                        contentDescription = null,
                        tint = NocturnePulseTheme.extendedColors.warning,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                    Text(
                        text = stringResource(R.string.subscription_payment_issue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NocturnePulseTheme.extendedColors.warning,
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.spacer_xs)),
                    )
                }
            } else InfoNote(text = stringResource(R.string.subscription_no_rollover))
        }
    }
}

@Composable
private fun StatusLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = TitleDimTextStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
