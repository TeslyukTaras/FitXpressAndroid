package com.hexis.bi.ui.main.scan.error

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.dark.BodyGlassCard
import com.hexis.bi.ui.dark.DarkOutlinedButton
import com.hexis.bi.ui.dark.DarkPrimaryButton
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.main.scan.components.ScanViewfinder
import com.hexis.bi.ui.theme.Yellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanErrorScreen(
    onBack: () -> Unit,
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    onShowHowToScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        modifier = modifier.darkScreenBackground(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.scan_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShowHowToScan) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.cd_info),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium))
                .navigationBarsPadding(),
        ) {
            ScanViewfinder(
                image = R.drawable.img_scan_fail,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            SuitNotDetectedCard(
                onConnectSuit = onConnectSuit,
                onBuySuit = onBuySuit,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
        }
    }
}

@Composable
private fun SuitNotDetectedCard(
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BodyGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = stringResource(R.string.cd_scan_warning),
                tint = Yellow,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = stringResource(R.string.scan_suit_not_detected_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Text(
            text = stringResource(R.string.scan_suit_not_detected_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            DarkOutlinedButton(
                text = stringResource(R.string.action_connect_suit),
                onClick = onConnectSuit,
                modifier = Modifier.weight(1f),
            )
            DarkPrimaryButton(
                text = stringResource(R.string.action_buy_suit),
                onClick = onBuySuit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
