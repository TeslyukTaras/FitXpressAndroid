package com.hexis.bi.ui.auth.signup

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withLink
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.auth.SignUpEvent
import com.hexis.bi.ui.auth.components.ContinueDivider
import com.hexis.bi.ui.auth.components.SocialAuthRow
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.components.AppTopBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SignUpViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignUpEvent.NavigateToHome -> onSignUpSuccess()
                is SignUpEvent.NavigateToLogin -> onNavigateToLogin()
                else -> Unit
            }
        }
    }

    BaseScreen(
        modifier = modifier,
        isLoading = isLoading,
        error = error,
        onDismissError = viewModel::clearError,
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
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_small)))
            Text(
                text = stringResource(R.string.signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            AppTextField(
                value = state.firstName,
                onValueChange = viewModel::updateFirstName,
                label = stringResource(R.string.label_first_name),
                placeholder = stringResource(R.string.placeholder_first_name),
                error = state.firstNameError,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            AppTextField(
                value = state.lastName,
                onValueChange = viewModel::updateLastName,
                label = stringResource(R.string.label_last_name),
                placeholder = stringResource(R.string.placeholder_last_name),
                error = state.lastNameError,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            AppTextField(
                value = state.email,
                onValueChange = viewModel::updateEmail,
                label = stringResource(R.string.label_email),
                placeholder = stringResource(R.string.placeholder_email),
                error = state.emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

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
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            AppTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = stringResource(R.string.label_confirm_password),
                placeholder = stringResource(R.string.placeholder_confirm_password),
                error = state.confirmPasswordError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            painter = painterResource(
                                if (state.isConfirmPasswordVisible) R.drawable.ic_eye_open else R.drawable.ic_eye_closed
                            ),
                            contentDescription = stringResource(R.string.cd_toggle_password),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            TermsRow(
                accepted = state.isTermsAccepted,
                onToggle = viewModel::toggleTerms,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            AppButton(
                text = stringResource(R.string.action_signup),
                onClick = viewModel::signUp,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            ContinueDivider()

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_large)))

            SocialAuthRow(
                onGoogleClick = { viewModel.signUpWithGoogle(context) },
                onAppleClick = { viewModel.signUpWithApple(context as Activity) },
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            val haveAccountText = buildAnnotatedString {
                append(stringResource(R.string.have_account))
                append(" ")
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "LOGIN",
                        styles = TextLinkStyles(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )),
                        linkInteractionListener = { viewModel.navigateToLogin() },
                    )
                ) {
                    append(stringResource(R.string.action_login))
                }
            }
            Text(
                text = haveAccountText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.secondary,
                ),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))
        }
    }
}

@Composable
private fun TermsRow(accepted: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
        ) {
            Icon(
                painter = painterResource(
                    if (accepted) R.drawable.ic_checkbox_check else R.drawable.ic_checkbox_uncheck
                ),
                contentDescription = null,
                tint = if (accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(Modifier.size(dimensionResource(R.dimen.spacer_small)))
        val termsText = buildAnnotatedString {
            append(stringResource(R.string.terms_text))
            append(" ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = "TERMS",
                    styles = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary)),
                    linkInteractionListener = { },
                )
            ) {
                append(stringResource(R.string.terms_link))
            }
            append(" ")
            append(stringResource(R.string.terms_and))
            append(" ")
            withLink(
                LinkAnnotation.Clickable(
                    tag = "PRIVACY",
                    styles = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary)),
                    linkInteractionListener = { },
                )
            ) {
                append(stringResource(R.string.privacy_link))
            }
        }
        Text(
            text = termsText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.secondary,
            ),
        )
    }
}
