package com.hexis.bi.ui.main.subscription.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.hexis.bi.R
import com.hexis.bi.domain.subscription.SubscriptionStatus
import com.hexis.bi.domain.subscription.allScansUsed
import com.hexis.bi.ui.components.AppSegmentedProgress
import com.hexis.bi.ui.theme.TitleDimTextStyle

@Composable
internal fun ScansUsage(
    status: SubscriptionStatus.Subscribed,
    barGap: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = if (status.allScansUsed) stringResource(R.string.subscription_all_scans_used)
            else stringResource(R.string.subscription_scans_used, status.scansUsed, status.scansAllowed),
            style = TitleDimTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(barGap))
        AppSegmentedProgress(filled = status.scansUsed, total = status.scansAllowed)
    }
}
