package com.hexis.bi.ui.main.settings.healthconnections

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hexis.bi.ui.theme.Green
import com.hexis.bi.utils.constants.TerraProviders
import org.koin.androidx.compose.koinViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun List<HealthConnection>.hasProvider(code: String): Boolean =
    any { it.provider.equals(code, ignoreCase = true) }

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

private fun Context.isHealthConnectInstalled(): Boolean =
    runCatching { packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, 0) }.isSuccess

private fun Context.openHealthConnectInstall() {
    val marketUri = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
    val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, marketUri))
    }.onFailure {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, webUri)) }
    }
}

private data class TerraProviderUi(
    val code: String,
    val label: String,
    @DrawableRes val iconRes: Int = R.drawable.ic_connect,
)

private val sdkProviders = listOf(
    TerraProviderUi(TerraProviders.HEALTH_CONNECT, "Health Connect", R.drawable.ic_google),
)

private val wearableProviders = listOf(
    TerraProviderUi(TerraProviders.OURA, "Oura"),
    TerraProviderUi("WHOOP", "Whoop"),
    TerraProviderUi("FITBIT", "Fitbit"),
    TerraProviderUi("GARMIN", "Garmin"),
    TerraProviderUi("POLAR", "Polar"),
    TerraProviderUi("COROS", "Coros"),
    TerraProviderUi("SUUNTO", "Suunto"),
    TerraProviderUi("WITHINGS", "Withings"),
    TerraProviderUi("ZEPP", "Zepp"),
    TerraProviderUi("BIOSTRAP", "Biostrap"),
    TerraProviderUi("HEALTHGAUGE", "Health Gauge"),
    TerraProviderUi("INBODY", "InBody"),
    TerraProviderUi("SOMNOFY", "Somnofy"),
    TerraProviderUi("CORE", "Core"),
    TerraProviderUi("MOXY", "Moxy Monitor"),
    TerraProviderUi("PUL", "Pul"),
    TerraProviderUi("OMRON", "Omron EU"),
    TerraProviderUi("CARDIOMOOD", "Cardiomood"),
)

private val otherProviders = listOf(
    TerraProviderUi("GOOGLE", "Google"),
    TerraProviderUi("PELOTON", "Peloton"),
    TerraProviderUi("WAHOO", "Wahoo"),
    TerraProviderUi("HAMMERHEAD", "Hammerhead"),
    TerraProviderUi("XOSS", "Xoss"),
    TerraProviderUi("BRYTONSPORT", "Bryton Sport"),
    TerraProviderUi("LEZYNE", "Lezyne"),
    TerraProviderUi("TECHNOGYM", "Technogym"),
    TerraProviderUi("CONCEPT2", "Concept2"),
    TerraProviderUi("DECATHLON", "Decathlon"),
    TerraProviderUi("CATAPULTONE", "Catapult One"),
    TerraProviderUi("TRIDOT", "Tridot"),
    TerraProviderUi("ULTRAHUMAN", "Ultrahuman"),
    TerraProviderUi("TRAININGPEAKS", "TrainingPeaks"),
    TerraProviderUi("TRAINERROAD", "TrainerRoad"),
    TerraProviderUi("FINALSURGE", "FinalSurge"),
    TerraProviderUi("ELITEHRV", "EliteHRV"),
    TerraProviderUi("HEVY", "Hevy"),
    TerraProviderUi("TRAINXHALE", "TrainXhale"),
    TerraProviderUi("TRAINASONE", "TrainAsOne"),
    TerraProviderUi("ROUVY", "Rouvy"),
    TerraProviderUi("ZWIFT", "Zwift"),
    TerraProviderUi("RIDEWITHGPS", "RideWithGPS"),
    TerraProviderUi("MAPMYTRACKS", "Map My Tracks"),
    TerraProviderUi("MAPMYFITNESS", "MapMyFitness"),
    TerraProviderUi("KOMOOT", "Komoot"),
    TerraProviderUi("CYCLINGANALYTICS", "Cycling Analytics"),
    TerraProviderUi("VELOHERO", "VeloHero"),
    TerraProviderUi("XERT", "Xert"),
    TerraProviderUi("TREDICT", "Tredict"),
    TerraProviderUi("UNDERARMOUR", "Under Armour"),
    TerraProviderUi("CLUE", "Clue"),
    TerraProviderUi("FLO", "Flo"),
    TerraProviderUi("WGER", "Wger"),
    TerraProviderUi("NUTRACHECK", "Nutracheck"),
    TerraProviderUi("MACROSFIRST", "MacrosFirst"),
    TerraProviderUi("MYMACROSPLUS", "My Macros+"),
    TerraProviderUi("MYFITNESSPAL", "MyFitnessPal"),
    TerraProviderUi("CRONOMETER", "Cronometer"),
    TerraProviderUi("FATSECRET", "Fatsecret"),
    TerraProviderUi("EATTHISMUCH", "EatThisMuch"),
    TerraProviderUi("KETOMOJOEU", "Ketomojo EU"),
    TerraProviderUi("KETOMOJOUS", "Ketomojo US"),
    TerraProviderUi("BODITRAX", "Boditrax"),
)

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

    LaunchedEffect(state.widgetUrl) {
        val url = state.widgetUrl ?: return@LaunchedEffect
        val launched = runCatching {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, url.toUri())
        }
        if (launched.isSuccess) {
            viewModel.onWidgetOpened()
        } else {
            viewModel.onWidgetLaunchFailed()
        }
    }

    BaseScreen(
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
        message = message,
        onDismissMessage = viewModel::clearMessage,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.screen_health_connections),
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
            Text(
                text = stringResource(R.string.health_connections_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.padding(top = dimensionResource(R.dimen.spacer_m)))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
            ) {
                ProviderSectionHeader(title = "SDK connections")
                sdkProviders.forEach { provider ->
                    HealthConnectionRow(
                        iconRes = provider.iconRes,
                        title = provider.label,
                        connected = state.wearableConnections.hasProvider(provider.code),
                        onClick = {
                            if (provider.code.equals(TerraProviders.HEALTH_CONNECT, ignoreCase = true) &&
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

                ProviderSectionHeader(title = "Wearables")
                wearableProviders.forEach { provider ->
                    HealthConnectionRow(
                        iconRes = R.drawable.ic_connect,
                        title = provider.label,
                        connected = state.wearableConnections.hasProvider(provider.code),
                        onClick = { viewModel.onWidgetProviderRowClick(provider.code, provider.label) },
                    )
                }

                ProviderSectionHeader(title = "Other apps")
                otherProviders.forEach { provider ->
                    HealthConnectionRow(
                        iconRes = R.drawable.ic_connect,
                        title = provider.label,
                        connected = state.wearableConnections.hasProvider(provider.code),
                        onClick = { viewModel.onWidgetProviderRowClick(provider.code, provider.label) },
                    )
                }

                if (TerraConfig.isSandbox) {
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

@Composable
private fun ProviderSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.secondary,
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(all = dimensionResource(R.dimen.spacer_l)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = null,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (connected) Green else MaterialTheme.colorScheme.primaryFixed
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
