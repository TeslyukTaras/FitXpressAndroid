package com.hexis.bi.ui.main.settings.mysuit

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.my_suit.ReconnectDialogContent
import com.hexis.bi.ui.components.my_suit.SuitConnectedBanner
import com.hexis.bi.ui.components.my_suit.SuitInfoRow
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppOutlinedTextField
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.theme.screenBackground
import org.koin.androidx.compose.koinViewModel
import com.hexis.bi.ui.theme.NocturnePulseTheme

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

    LightStatusBarIcons()

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showReconnectDialog) Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.my_suit_title),
                    background = Color.Transparent,
                    onBack = onBack,
                )
            },
        ) {
            var imageHeight by remember { mutableStateOf(Dp.Unspecified) }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (imageHeight == Dp.Unspecified) {
                    imageHeight = maxHeight * 0.55f
                }

                if (state.isConnected) ConnectedContent(
                    state = state,
                    imageHeight = imageHeight,
                    onReconnect = viewModel::showReconnectDialog,
                )
                else DisconnectedContent(
                    suitIdInput = state.suitIdInput,
                    imageHeight = imageHeight,
                    onSuitIdChange = viewModel::updateSuitIdInput,
                    onConnect = viewModel::connect,
                    onBuyOne = onBuyOne,
                )
            }
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
    imageHeight: Dp,
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

        SuitConnectedBanner()

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Image(
            painter = painterResource(R.drawable.img_my_suit),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        SuitInfoRow(
            label = stringResource(R.string.label_suit_id),
            value = state.connectedSuitId,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        SuitInfoRow(
            label = stringResource(R.string.my_suit_status),
            value = state.connectedStatus,
            valueColor = NocturnePulseTheme.extendedColors.positive,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        Spacer(Modifier.weight(1f))

        BottomButtonAction {
            AppOutlinedButton(
                text = stringResource(R.string.action_reconnect_suit),
                onClick = onReconnect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DisconnectedContent(
    suitIdInput: String,
    imageHeight: Dp,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Image(
            painter = painterResource(R.drawable.img_my_suit),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        AppOutlinedTextField(
            value = suitIdInput,
            onValueChange = onSuitIdChange,
            label = stringResource(R.string.label_suit_id),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))

        Text(
            text = stringResource(R.string.my_suit_id_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        Spacer(Modifier.weight(1f))

        BottomButtonAction(onBuyOne) {
            AppPrimaryButton(
                text = stringResource(R.string.action_connect),
                onClick = onConnect,
                enabled = suitIdInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BottomButtonAction(
    onBuyOne: (() -> Unit)? = null,
    button: @Composable () -> Unit
) {
    button()

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

    val noSuitText = buildAnnotatedString {
        if (onBuyOne != null) {
            append(stringResource(R.string.my_suit_no_suit))
            append(" ")
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            ) { append(stringResource(R.string.my_suit_buy_one)) }
        }
    }
    Text(
        text = noSuitText,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        minLines = 1,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onBuyOne ?: {}),
    )
}
