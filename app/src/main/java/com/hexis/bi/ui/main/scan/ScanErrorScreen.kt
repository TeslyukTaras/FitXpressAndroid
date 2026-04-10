package com.hexis.bi.ui.main.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.theme.OneTimeGrey
import com.hexis.bi.ui.theme.Yellow

@Composable
fun ScanErrorScreen(
    onBack: () -> Unit,
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BaseTopBar(
                title = stringResource(R.string.scan_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = stringResource(R.string.cd_close),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
        ) {
            Image(
                painter = painterResource(R.drawable.img_scan_error),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            SuitNotDetectedCard(
                onConnectSuit = onConnectSuit,
                onBuySuit = onBuySuit,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        }
    }
}

@Composable
private fun SuitNotDetectedCard(
    onConnectSuit: () -> Unit,
    onBuySuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(OneTimeGrey)
            .padding(dimensionResource(R.dimen.spacer_m)),
    ) {
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
            color = MaterialTheme.colorScheme.secondary,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            AppOutlinedButton(
                text = stringResource(R.string.action_connect_suit),
                onClick = onConnectSuit,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = stringResource(R.string.action_buy_suit),
                onClick = onBuySuit,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
