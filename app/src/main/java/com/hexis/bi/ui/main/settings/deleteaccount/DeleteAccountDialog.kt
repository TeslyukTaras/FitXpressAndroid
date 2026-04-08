package com.hexis.bi.ui.main.settings.deleteaccount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.hexis.bi.R
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.theme.Red300

@Composable
fun DeleteAccountDialog(
    state: DeleteAccountState,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    val isEmailProvider = state.provider == AuthProvider.EMAIL

    AppDialog(
        hasCloseButton = true,
        onDismiss = onDismiss,
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_large)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.delete_account_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

                Text(
                    text = stringResource(
                        if (isEmailProvider) R.string.delete_account_dialog_body_password
                        else R.string.delete_account_dialog_body_confirm
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                if (isEmailProvider) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                    AppTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.label_password),
                        error = state.passwordError ?: error,
                        reserveErrorSpace = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = if (state.passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    painter = painterResource(
                                        if (state.passwordVisible) R.drawable.ic_eye_open
                                        else R.drawable.ic_eye_closed
                                    ),
                                    contentDescription = stringResource(R.string.cd_toggle_password),
                                    modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (error != null) {
                    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
                ) {
                    AppOutlinedButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = onCancel,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f),
                    )
                    AppButton(
                        text = stringResource(R.string.delete_account_dialog_action_delete),
                        onClick = onDelete,
                        enabled = !isLoading,
                        containerColor = Red300,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (isLoading) Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Red300)
            }
        }
    }
}
