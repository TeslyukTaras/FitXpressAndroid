package com.hexis.bi.ui.main.settings.healthconnections

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.HealthProvider
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.theme.Green
import org.koin.androidx.compose.koinViewModel

private data class HealthConnectionItem(
    val provider: HealthProvider,
    @DrawableRes val iconRes: Int,
    @StringRes val nameRes: Int,
)

private val healthConnections = listOf(
    HealthConnectionItem(
        HealthProvider.AppleHealth,
        R.drawable.ic_apple,
        R.string.health_connection_apple
    ),
    HealthConnectionItem(
        HealthProvider.GoogleHealth,
        R.drawable.ic_google,
        R.string.health_connection_google
    ),
)

@Composable
fun HealthConnectionsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HealthConnectionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BaseScreen(
        modifier = modifier,
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
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m))
            ) {
                healthConnections.forEach { item ->
                    HealthConnectionRow(
                        item = item,
                        connected = item.provider in state.connectedProviders,
                        onClick = { viewModel.toggleConnection(item.provider) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthConnectionRow(
    item: HealthConnectionItem,
    connected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(all = dimensionResource(R.dimen.spacer_l)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            tint = null,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.nameRes),
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
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                Text(
                    text = if (connected) stringResource(R.string.health_connection_status_connected)
                    else stringResource(R.string.health_connection_status_not_connected),
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
