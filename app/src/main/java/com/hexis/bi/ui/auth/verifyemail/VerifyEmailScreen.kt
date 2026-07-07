package com.hexis.bi.ui.auth.verifyemail

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.auth.components.AuthScreenScaffold
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOtpInput
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.TitleDimTextStyle
import com.hexis.bi.utils.constants.AuthFlowConstants
import org.koin.androidx.compose.koinViewModel

@Composable
fun VerifyEmailScreen(
    onNavigateBack: () -> Unit,
    onVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyEmailViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.runOnceOnInitialize() }

    Box(modifier = modifier.fillMaxSize()) {
        AuthScreenScaffold(
            modifier = if (state.showSuccessDialog) {
                Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
            } else {
                Modifier
            },
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            onBack = onNavigateBack,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.verify_email_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                    color = NocturnePulseTheme.extendedColors.textEmphasis,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                Text(
                    text = stringResource(
                        if (state.hasResent) R.string.verify_email_subtitle_resent
                        else R.string.verify_email_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xxl)))

            AppOtpInput(
                value = state.code,
                onValueChange = viewModel::updateCode,
                length = AuthFlowConstants.EMAIL_CODE_LENGTH,
                isError = state.isCodeError,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

            Text(
                text = state.inlineError ?: stringResource(R.string.verify_email_hint),
                style = TitleDimTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_3xl)))

            AppPrimaryButton(
                text = stringResource(R.string.action_verify),
                onClick = viewModel::verify,
                enabled = state.isCodeComplete && !state.isCodeError,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            ResendRow(
                secondsLeft = state.resendSecondsLeft,
                hasResent = state.hasResent,
                enabled = state.canResend,
                onResend = viewModel::resendCode,
            )
        }

        if (state.showSuccessDialog) AppDialog(onDismiss = onVerified) {
            VerifyEmailSuccessDialogContent(onContinue = onVerified)
        }
    }
}

@Composable
private fun ResendRow(
    secondsLeft: Int,
    hasResent: Boolean,
    enabled: Boolean,
    onResend: () -> Unit,
) {
    when {
        secondsLeft > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.verify_email_resend_countdown_prefix),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.verify_email_resend_seconds, secondsLeft),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        !hasResent -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.verify_email_resend_prompt),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_s)))
            Text(
                text = stringResource(R.string.action_resend_code),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = enabled, onClick = onResend),
            )
        }

        else -> Text(
            text = stringResource(R.string.action_resend_code_full),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(enabled = enabled, onClick = onResend),
        )
    }
}

@Composable
private fun VerifyEmailSuccessDialogContent(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.padding_large)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_done_verification),
            contentDescription = null,
            tint = NocturnePulseTheme.extendedColors.positive,
            modifier = Modifier.size(dimensionResource(R.dimen.size_tick_icon)),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        Text(
            text = stringResource(R.string.verify_email_success_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

        Text(
            text = stringResource(R.string.verify_email_success_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        AppPrimaryButton(
            text = stringResource(R.string.action_continue),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
