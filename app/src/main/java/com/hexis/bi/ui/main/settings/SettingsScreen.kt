package com.hexis.bi.ui.main.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.BuildConfig
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.settings.deleteaccount.AuthProvider
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountDialog
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountEvent
import com.hexis.bi.ui.main.settings.deleteaccount.DeleteAccountViewModel
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.legal.LegalUrls
import org.koin.androidx.compose.koinViewModel

private data class SettingsRow(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val showChevron: Boolean = true,
    val iconTint: Color = Color.Unspecified,
    val textColor: Color = Color.Unspecified,
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
    onDeleteAccount: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {},
    onNavigateToHealthConnections: () -> Unit = {},
    onNavigateToScanPreferences: () -> Unit = {},
    onNavigateToMySuit: () -> Unit = {},
    onNavigateToHowToScan: () -> Unit = {},
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is DeleteAccountEvent.DeleteSuccess) onDeleteAccount()
        }
    }

    val onDelete: () -> Unit = when (state.provider) {
        AuthProvider.EMAIL -> viewModel::deleteAccountWithPassword
        AuthProvider.GOOGLE -> { -> viewModel.deleteAccountWithGoogle(context) }
        AuthProvider.APPLE -> { -> viewModel.deleteAccountWithApple(context as Activity) }
        AuthProvider.UNKNOWN -> { -> }
    }

    val groups = buildSettingsGroups(
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToNotificationSettings = onNavigateToNotificationSettings,
        onNavigateToHealthConnections = onNavigateToHealthConnections,
        onNavigateToMySuit = onNavigateToMySuit,
        onNavigateToScanPreferences = onNavigateToScanPreferences,
        onNavigateToHowToScan = onNavigateToHowToScan,
        onOpenHelp = {
            openSupportEmail(
                context = context,
                subject = context.getString(R.string.support_email_subject),
                includeDiagnostics = false,
            )
        },
        onReportProblem = {
            openSupportEmail(
                context = context,
                subject = context.getString(R.string.problem_report_email_subject),
                includeDiagnostics = true,
            )
        },
        onOpenTerms = { uriHandler.openUri(LegalUrls.TERMS_AND_CONDITIONS) },
        onShowDeleteDialog = viewModel::showDialog,
        onLogout = onLogout,
    )

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showDialog) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.screen_settings),
                    background = Color.Transparent,
                    onBack = onBack,
                )
            },
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups) { group -> SettingsGroupSection(group) }
                item { Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl))) }
            }
        }

        if (state.showDialog) {
            DeleteAccountDialog(
                state = state,
                isLoading = isLoading,
                error = error,
                onDismiss = viewModel::dismissDialog,
                onPasswordChange = viewModel::updatePassword,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onCancel = viewModel::dismissDialog,
                onDelete = onDelete,
            )
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

    BodyGlassCard(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        contentPadding = PaddingValues(dimensionResource(R.dimen.spacer_xs)),
    ) {
        group.items.forEach { row -> SettingsRowItem(row) }
    }
}

@Composable
private fun SettingsRowItem(row: SettingsRow) {
    val iconTint = row.iconTint.takeOrElse { MaterialTheme.colorScheme.primary }
    val textColor = row.textColor.takeOrElse { MaterialTheme.colorScheme.onBackground }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = row.onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_xs),
                vertical = dimensionResource(R.dimen.spacer_m),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(row.iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Text(
            text = stringResource(row.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(R.dimen.spacer_m)),
        )
        if (row.showChevron) Icon(
            painter = painterResource(R.drawable.ic_arrow),
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
    }
}

@Composable
private fun buildSettingsGroups(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToHealthConnections: () -> Unit,
    onNavigateToMySuit: () -> Unit,
    onNavigateToScanPreferences: () -> Unit,
    onNavigateToHowToScan: () -> Unit,
    onOpenHelp: () -> Unit,
    onReportProblem: () -> Unit,
    onOpenTerms: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onLogout: () -> Unit,
): List<SettingsGroup> {
    val primary = MaterialTheme.colorScheme.primary
    val destructive = NocturnePulseTheme.extendedColors.destructive
    return listOf(
        SettingsGroup(
            titleRes = R.string.settings_group_account,
            items = listOf(
                SettingsRow(
                    R.drawable.ic_user,
                    R.string.settings_edit_profile,
                    onClick = onNavigateToEditProfile
                ),
                SettingsRow(
                    R.drawable.ic_bell,
                    R.string.settings_notifications,
                    onClick = onNavigateToNotificationSettings
                ),
                SettingsRow(
                    R.drawable.ic_connect,
                    R.string.settings_health_connections,
                    onClick = onNavigateToHealthConnections
                ),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_suit_scanning,
            items = listOf(
                SettingsRow(
                    R.drawable.ic_body,
                    R.string.settings_my_suit,
                    onClick = onNavigateToMySuit
                ),
                SettingsRow(
                    R.drawable.ic_scan,
                    R.string.settings_scan_preferences,
                    onClick = onNavigateToScanPreferences
                ),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_support_about,
            items = listOf(
                SettingsRow(
                    R.drawable.ic_info,
                    R.string.settings_how_scanning_works,
                    onClick = onNavigateToHowToScan
                ),
                SettingsRow(
                    R.drawable.ic_help,
                    R.string.settings_help,
                    onClick = onOpenHelp,
                ),
                SettingsRow(
                    R.drawable.ic_lock,
                    R.string.settings_terms_privacy,
                    onClick = onOpenTerms
                ),
                SettingsRow(
                    R.drawable.ic_warning,
                    R.string.settings_report_problem,
                    onClick = onReportProblem,
                ),
            ),
        ),
        SettingsGroup(
            titleRes = R.string.settings_group_actions,
            items = listOf(
                SettingsRow(
                    iconRes = R.drawable.ic_trash,
                    labelRes = R.string.settings_delete_account,
                    showChevron = false,
                    iconTint = destructive,
                    textColor = destructive,
                    onClick = onShowDeleteDialog,
                ),
                SettingsRow(
                    iconRes = R.drawable.ic_log_out,
                    labelRes = R.string.settings_logout,
                    showChevron = false,
                    iconTint = primary,
                    textColor = primary,
                    onClick = onLogout,
                ),
            ),
        ),
    )
}

private fun openSupportEmail(
    context: Context,
    subject: String,
    includeDiagnostics: Boolean,
) {
    val recipient = context.getString(R.string.support_email_address)
    val body = if (includeDiagnostics) {
        val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter(String::isNotBlank)
            .joinToString(" ")
            .replaceFirstChar { it.uppercase() }
        context.getString(
            R.string.support_email_body,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.ENVIRONMENT,
            deviceModel,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
        )
    } else {
        ""
    }
    val emailUri = Uri.parse(
        "mailto:$recipient?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    val intent = Intent(Intent.ACTION_SENDTO, emailUri).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT).show()
    }
}
