package com.hexis.bi.ui.auth.login

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.auth.LoginEvent
import com.hexis.bi.ui.auth.components.ContinueDivider
import com.hexis.bi.ui.auth.components.SocialAuthRow
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.components.AppTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LoginEvent.NavigateToHome -> onLoginSuccess()
                is LoginEvent.NavigateToSignUp -> onNavigateToSignUp()
                else -> Unit
            }
        }
    }

    BaseScreen(
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
        message = message,
        onDismissMessage = viewModel::clearMessage,
        topBar = { AppTopBar() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.padding_medium)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            AppTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.placeholder_email),
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            AppTextField(
                value = state.password,
                onValueChange = viewModel::updatePassword,
                label = stringResource(R.string.label_password),
                placeholder = stringResource(R.string.placeholder_password),
                error = state.passwordError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            painter = painterResource(
                                if (state.isPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
                            ),
                            contentDescription = stringResource(R.string.cd_toggle_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            Text(
                text = stringResource(R.string.action_forgot_password),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { onForgotPassword() },
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            AppButton(
                text = stringResource(R.string.action_login),
                onClick = viewModel::login,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            ContinueDivider()

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            SocialAuthRow(
                onGoogleClick = { viewModel.loginWithGoogle(context) },
                onAppleClick = { viewModel.loginWithApple(context as Activity) },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

            val noAccountText = buildAnnotatedString {
                append(stringResource(R.string.no_account))
                append(" ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "SIGNUP",
                        styles = TextLinkStyles(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        ),
                        linkInteractionListener = { viewModel.navigateToSignUp() },
                    )
                ) {
                    append(stringResource(R.string.action_signup))
                }
            }
            Text(
                text = noAccountText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
        }
    }
}
