package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.main.home.SuitOrderOverview
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.ui.theme.dark.Positive

@Composable
fun SuitOrderCard(
    data: SuitOrderOverview,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .clip(CircleShape)
                        .background(Positive),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tick),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                    )
                }
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
                Text(
                    text = stringResource(R.string.home_suit_order_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = data.status,
                    style = TitleDimTextStyle,
                    color = Positive,
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            Row(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.referenceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
                    Text(
                        text = data.referenceValue,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.home_suit_order_eta_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = data.eta ?: stringResource(R.string.home_suit_order_eta_estimating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
