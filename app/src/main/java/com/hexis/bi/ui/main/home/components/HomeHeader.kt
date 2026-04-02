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
import androidx.compose.material3.IconButtonDefaults
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

@Composable
fun HomeHeader(
    userName: String,
    avatarUrl: String?,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppAvatar(imageUrl = avatarUrl)

        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_welcome),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        IconButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_header_button))
                .align(Alignment.Top),
            onClick = onNotificationClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            BadgedBox(
                modifier = Modifier.fillMaxSize(),
                badge = {
                    Badge(
                        modifier.size(dimensionResource(R.dimen.size_indicator)),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bell),
                    contentDescription = stringResource(R.string.cd_notification),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
        }
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_2xs)))
        IconButton(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_header_button))
                .align(Alignment.Top),
            onClick = onSettingsClick,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}
