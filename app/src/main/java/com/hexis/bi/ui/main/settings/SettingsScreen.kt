package com.hexis.bi.ui.main.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar

private data class SettingsRow(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val showChevron: Boolean = true,
    val tint: @Composable () -> Color = { MaterialTheme.colorScheme.onBackground },
    val onClick: () -> Unit = {},
)

private data class SettingsGroup(
    @StringRes val titleRes: Int,
    val items: List<SettingsRow>,
)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToHealthConnections: () -> Unit = {},
    onNavigateToScanPreferences: () -> Unit = {},
    onNavigateToMySuit: () -> Unit = {},
) {
    val groups = listOf(
        SettingsGroup(
            titleRes = R.string.settings_group_account,
            items = listOf(
                SettingsRow(R.drawable.ic_user, R.string.settings_edit_profile, onClick = onNavigateToEditProfile),
                SettingsRow(R.drawable.ic_bell, R.string.settings_notifications, onClick = onNavigateToNotificationSettings),
                SettingsRow(R.drawable.ic_connect, R.string.settings_health_connections, onClick = onNavigateToHealthConnections),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_suit_scanning,
            items = listOf(
                SettingsRow(R.drawable.ic_body, R.string.settings_my_suit, onClick = onNavigateToMySuit),
                SettingsRow(R.drawable.ic_scan, R.string.settings_scan_preferences, onClick = onNavigateToScanPreferences),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_support_about,
            items = listOf(
                SettingsRow(R.drawable.ic_info, R.string.settings_how_scanning_works),
                SettingsRow(R.drawable.ic_help, R.string.settings_help),
                SettingsRow(R.drawable.ic_lock, R.string.settings_terms_privacy),
                SettingsRow(R.drawable.ic_warning, R.string.settings_report_problem),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_actions,
            items = listOf(
                SettingsRow(
                    iconRes = R.drawable.ic_trash,
                    labelRes = R.string.settings_delete_account,
                    showChevron = false,
                    tint = { MaterialTheme.colorScheme.error },
                ),
                SettingsRow(
                    iconRes = R.drawable.ic_log_out,
                    labelRes = R.string.settings_logout,
                    showChevron = false,
                    tint = { MaterialTheme.colorScheme.primary },
                    onClick = onLogout,
                ),
            ),
        ),
    )

    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.screen_settings),
                onBack = onBack,
            )
        },
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(groups) { group ->
                SettingsGroupSection(group)
            }
            item { Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl))) }
        }
    }
}

@Composable
private fun SettingsGroupSection(group: SettingsGroup) {
    Text(
        text = stringResource(group.titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(
            start = dimensionResource(R.dimen.padding_medium),
            end = dimensionResource(R.dimen.padding_medium),
            top = dimensionResource(R.dimen.spacer_l),
            bottom = dimensionResource(R.dimen.spacer_m),
        ),
    )

    Column(
        modifier = Modifier
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_xs),
                vertical = dimensionResource(R.dimen.spacer_xs)
            )
    ) {
        group.items.forEach { row -> SettingsRowItem(row) }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow) {
    val tint = row.tint()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = row.onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_xs),
                vertical = dimensionResource(R.dimen.spacer_xs)
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(row.iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Text(
            text = stringResource(row.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(R.dimen.spacer_l)),
        )
        if (row.showChevron) Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}
