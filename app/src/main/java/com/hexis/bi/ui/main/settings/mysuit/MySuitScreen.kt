package com.hexis.bi.ui.main.settings.mysuit

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.theme.Green
import org.koin.androidx.compose.koinViewModel

@Composable
fun MySuitScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onBuyOne: () -> Unit = {},
    viewModel: MySuitViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier.then(
                if (state.showReconnectDialog) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                else Modifier
            ),
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.my_suit_title),
                    onBack = onBack,
                )
            },
        ) {
            if (state.isConnected) ConnectedContent(
                state = state,
                onReconnect = viewModel::showReconnectDialog,
            )
            else DisconnectedContent(
                suitIdInput = state.suitIdInput,
                onSuitIdChange = viewModel::updateSuitIdInput,
                onConnect = viewModel::connect,
                onBuyOne = onBuyOne,
            )
        }

        if (state.showReconnectDialog) AppDialog(onDismiss = viewModel::dismissReconnectDialog) {
            ReconnectDialogContent(
                onDismiss = viewModel::dismissReconnectDialog,
                onConfirm = viewModel::reconnect,
            )
        }
    }
}

@Composable
private fun ConnectedContent(
    state: MySuitState,
    onReconnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        ConnectedBanner()

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Box(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_medium))) {
            Image(
                painter = painterResource(R.drawable.img_my_suit),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        SuitInfoRow(
            label = stringResource(R.string.label_suit_id),
            value = state.connectedSuitId,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        SuitInfoRow(
            label = stringResource(R.string.my_suit_status),
            value = state.connectedStatus,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        AppOutlinedButton(
            text = stringResource(R.string.action_reconnect_suit),
            onClick = onReconnect,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
    }
}

@Composable
private fun DisconnectedContent(
    suitIdInput: String,
    onSuitIdChange: (String) -> Unit,
    onConnect: () -> Unit,
    onBuyOne: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Text(
            text = stringResource(R.string.my_suit_connect_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        Text(
            text = stringResource(R.string.my_suit_connect_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Box(modifier = Modifier.padding(vertical = dimensionResource(R.dimen.padding_medium))) {
            Image(
                painter = painterResource(R.drawable.img_my_suit),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        AppTextField(
            value = suitIdInput,
            onValueChange = onSuitIdChange,
            label = stringResource(R.string.label_suit_id),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Text(
            text = stringResource(R.string.my_suit_id_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        AppButton(
            text = stringResource(R.string.action_connect),
            onClick = onConnect,
            enabled = suitIdInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        val noSuitText = buildAnnotatedString {
            append(stringResource(R.string.my_suit_no_suit))
            append(" ")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            ) { append(stringResource(R.string.my_suit_buy_one)) }
        }
        Text(
            text = noSuitText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onBuyOne),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
    }
}

@Composable
private fun ConnectedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(
                horizontal = dimensionResource(R.dimen.spacer_l),
                vertical = dimensionResource(R.dimen.spacer_m)
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
    ) {
        Box(
            Modifier
                .size(dimensionResource(R.dimen.icon_medium))
                .clip(CircleShape)
                .background(Green)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tick),
                contentDescription = stringResource(R.string.cd_suit_connected),
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.my_suit_connected_banner_title),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
            Text(
                text = stringResource(R.string.my_suit_connected_banner_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun SuitInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = stringResource(R.string.colon),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacer_xl)))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ReconnectDialogContent(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_large)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.my_suit_reconnect_dialog_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.my_suit_reconnect_dialog_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.my_suit_reconnect_dialog_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
        ) {
            AppOutlinedButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = stringResource(R.string.action_reconnect),
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
