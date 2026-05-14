package com.hexis.bi.ui.main.settings.healthconnections

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.data.healthconnections.HealthConnection
import com.hexis.bi.data.terra.TerraConfig
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.dark.AppHorizontalGradientDivider
import com.hexis.bi.ui.dark.AppSearchField
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.dark.LightStatusBarIcons
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.ui.theme.dark.Positive
import com.hexis.bi.utils.constants.HealthConnectConstants
import com.hexis.bi.utils.constants.TerraProviders
import org.koin.androidx.compose.koinViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun List<HealthConnection>.hasProvider(code: String): Boolean =
    any { TerraProviders.storedMatchesUi(it.provider, code) }

private fun Context.isHealthConnectInstalled(): Boolean =
    runCatching { packageManager.getPackageInfo(HealthConnectConstants.PACKAGE_NAME, 0) }.isSuccess

private fun Context.openHealthConnectInstall() {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, HealthConnectConstants.MARKET_URI.toUri()))
    }.onFailure {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, HealthConnectConstants.PLAY_STORE_URI.toUri()))
        }
    }
}

private fun List<TerraProviderUi>.filterByQuery(query: String): List<TerraProviderUi> =
    if (query.isBlank()) this
    else filter { it.label.contains(query.trim(), ignoreCase = true) }

@Composable
fun HealthConnectionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthConnectionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val scrollState = rememberScrollState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(state.widgetUrl) {
        val url = state.widgetUrl ?: return@LaunchedEffect
        val launched = runCatching {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, url.toUri())
        }
        if (launched.isSuccess) viewModel.onWidgetOpened()
        else viewModel.onWidgetLaunchFailed()
    }

    val filteredSdk = state.sdkProviders.filterByQuery(searchQuery)
    val filteredWearables = state.wearableProviders.filterByQuery(searchQuery)
    val filteredOther = state.otherProviders.filterByQuery(searchQuery)

    val trimmedQuery = searchQuery.trim()
    val hasSearchNoResults = trimmedQuery.isNotEmpty() &&
            filteredSdk.isEmpty() &&
            filteredWearables.isEmpty() &&
            filteredOther.isEmpty()

    LightStatusBarIcons()

    DarkTheme {
    BaseScreen(
        modifier = modifier
            .fillMaxSize()
            .darkScreenBackground(),
        containerColor = Color.Transparent,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
        message = message,
        onDismissMessage = viewModel::clearMessage,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.screen_health_connections),
                background = Color.Transparent,
                onBack = onBack,
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .padding(top = dimensionResource(R.dimen.padding_medium)),
        ) {
            AppSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
            )

            Spacer(modifier = Modifier.padding(top = dimensionResource(R.dimen.spacer_l)))

            if (hasSearchNoResults) HealthConnectionsSearchEmptyState(query = trimmedQuery)
            else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
                ) {
                    if (searchQuery.isBlank()) Text(
                        text = stringResource(R.string.health_connections_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    var needsDividerBeforeNext = false

                    if (filteredSdk.isNotEmpty()) {
                        filteredSdk.forEach { provider ->
                            key(provider.code) {
                                HealthConnectionRow(
                                    iconRes = provider.iconRes,
                                    title = provider.label,
                                    connected = state.wearableConnections.hasProvider(provider.code),
                                    onClick = {
                                        if (provider.code.equals(
                                                TerraProviders.HEALTH_CONNECT,
                                                ignoreCase = true
                                            ) &&
                                            !context.isHealthConnectInstalled()
                                        ) {
                                            context.openHealthConnectInstall()
                                        } else {
                                            viewModel.onSdkProviderRowClick(
                                                provider = provider.code,
                                                displayName = provider.label,
                                                activity = activity,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                        needsDividerBeforeNext = true
                    }

                    if (filteredWearables.isNotEmpty()) {
                        if (needsDividerBeforeNext) HealthConnectionsSectionDivider()
                        filteredWearables.forEach { provider ->
                            key(provider.code) {
                                HealthConnectionRow(
                                    iconRes = provider.iconRes,
                                    title = provider.label,
                                    connected = state.wearableConnections.hasProvider(provider.code),
                                    onClick = {
                                        viewModel.onWidgetProviderRowClick(
                                            provider.code,
                                            provider.label
                                        )
                                    },
                                )
                            }
                        }
                        needsDividerBeforeNext = true
                    }

                    if (filteredOther.isNotEmpty()) {
                        if (needsDividerBeforeNext) HealthConnectionsSectionDivider()
                        filteredOther.forEach { provider ->
                            key(provider.code) {
                                HealthConnectionRow(
                                    iconRes = provider.iconRes,
                                    title = provider.label,
                                    connected = state.wearableConnections.hasProvider(provider.code),
                                    onClick = {
                                        viewModel.onWidgetProviderRowClick(
                                            provider.code,
                                            provider.label
                                        )
                                    },
                                )
                            }
                        }
                        needsDividerBeforeNext = true
                    }

                    if (TerraConfig.isSandbox && searchQuery.isBlank()) {
                        if (needsDividerBeforeNext) HealthConnectionsSectionDivider()
                        key(TerraProviders.DUMMY) {
                            HealthConnectionRow(
                                iconRes = R.drawable.ic_connect,
                                title = stringResource(R.string.health_connection_dummy),
                                connected = state.wearableConnections.hasProvider(TerraProviders.DUMMY),
                                onClick = { viewModel.onDummyRowClick() },
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HealthConnectionsSectionDivider() {
    AppHorizontalGradientDivider()
}

@Composable
private fun HealthConnectionsSearchEmptyState(query: String) {
    Text(
        text = stringResource(R.string.health_connections_empty_search_message, query),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HealthConnectionRow(
    @DrawableRes iconRes: Int,
    title: String,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    connectedLabel: String? = null,
) {
    BodyGlassCard(
        modifier = modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacer_l),
            vertical = dimensionResource(R.dimen.spacer_m),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            key(iconRes) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                )
            }
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor =
                        if (connected) Positive else MaterialTheme.colorScheme.onSurfaceVariant
                    Box(
                        modifier = Modifier
                            .size(dimensionResource(R.dimen.size_indicator))
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                    Text(
                        text = connectedLabel ?: if (connected) {
                            stringResource(R.string.health_connection_status_connected)
                        } else {
                            stringResource(R.string.health_connection_status_not_connected)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
    }
}
