package com.hexis.bi.ui.main.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppAvatar
import com.hexis.bi.ui.theme.DefaultExtendedColors

@Composable
fun HomeHeader(
    userName: String,
    imageUrl: String?,
    hasUnreadNotifications: Boolean,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(imageUrl = imageUrl)

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_normalized))
                .align(Alignment.Top),
            onClick = onNotificationClick,
        ) {
            BadgedBox(
                modifier = Modifier.fillMaxSize(),
                badge = {
                    if (hasUnreadNotifications) Badge(
                        modifier.size(dimensionResource(R.dimen.size_indicator)),
                        containerColor = DefaultExtendedColors.positive,
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = stringResource(R.string.cd_notification),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_3xs)))
        IconButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.icon_normalized))
                .align(Alignment.Top),
            onClick = onSettingsClick,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}
