package com.hexis.bi.ui.main.settings.editprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.hexis.bi.R
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDatePicker
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.components.AppOtpInput
import com.hexis.bi.ui.components.AppOutlinedButton
import com.hexis.bi.ui.components.AppOutlinedTextField
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.components.AppSlider
import com.hexis.bi.ui.components.BodyGlassCard
import com.hexis.bi.ui.components.LightStatusBarIcons
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleChip
import com.hexis.bi.ui.main.body.components.BodySegmentedToggleTrack
import com.hexis.bi.ui.theme.NocturnePulseTheme
import com.hexis.bi.ui.theme.screenBackground
import com.hexis.bi.utils.constants.AuthFlowConstants
import com.hexis.bi.utils.constants.ProfileConstants
import com.hexis.bi.utils.parseDob
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    LightStatusBarIcons()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProfileEvent.SaveSuccess -> onBack()
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.uploadAvatar(uri) }

    val currentYear = remember { LocalDate.now().year }
    val datePickerState = rememberDatePickerState(
        // A date of birth can never be in the future.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= System.currentTimeMillis()

            override fun isSelectableYear(year: Int): Boolean = year <= currentYear
        },
    )
    LaunchedEffect(state.dateOfBirth) {
        if (state.dateOfBirth.isNotEmpty()) {
            datePickerState.selectedDateMillis = state.dateOfBirth.parseDob()?.time
        }
    }

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showDatePicker || state.showChangeEmailDialog || state.showEnterCodeDialog)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            message = message,
            onDismissMessage = viewModel::clearMessage,
            viewModel = viewModel,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.edit_profile_title),
                    background = Color.Transparent,
                    onBack = onBack,
                )
            },
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

                AvatarPicker(
                    imageUrl = state.imageUrl,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
                ) {
                    AppOutlinedTextField(
                        value = state.firstName,
                        onValueChange = viewModel::updateFirstName,
                        label = stringResource(R.string.label_first_name),
                        placeholder = stringResource(R.string.label_first_name),
                        modifier = Modifier.weight(1f),
                    )
                    AppOutlinedTextField(
                        value = state.lastName,
                        onValueChange = viewModel::updateLastName,
                        label = stringResource(R.string.label_last_name),
                        placeholder = stringResource(R.string.label_last_name),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                EmailField(
                    email = state.email,
                    onChangeClick = viewModel::openChangeEmailDialog,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                Box(modifier = Modifier.fillMaxWidth()) {
                    AppOutlinedTextField(
                        value = state.dateOfBirth,
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.label_date_of_birth),
                        placeholder = stringResource(R.string.placeholder_date_of_birth),
                        error = if (state.isDobUnderage) {
                            stringResource(
                                R.string.dob_min_age_error,
                                ProfileConstants.MIN_AGE_YEARS,
                            )
                        } else {
                            null
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(onClick = viewModel::showDatePicker),
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                GenderField(
                    selected = state.gender,
                    onSelect = viewModel::selectGender,
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                PersonalInfoSection(state = state, viewModel = viewModel)

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                AppPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    isLoading = isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))
            }
        }

        if (state.showDatePicker) AppDatePicker(
            state = datePickerState,
            onDismissRequest = viewModel::hideDatePicker,
            onSelect = viewModel::updateDateOfBirth,
        )

        if (state.showChangeEmailDialog) AppDialog(onDismiss = viewModel::dismissChangeEmailDialog) {
            ChangeEmailDialogContent(
                state = state,
                isLoading = isLoading,
                onNewEmailChange = viewModel::updateNewEmail,
                onPasswordChange = viewModel::updateChangePassword,
                onTogglePasswordVisibility = viewModel::toggleChangePasswordVisibility,
                onCancel = viewModel::dismissChangeEmailDialog,
                onSendCode = viewModel::sendChangeCode,
            )
        }

        if (state.showEnterCodeDialog) AppDialog(onDismiss = viewModel::dismissEnterCodeDialog) {
            EnterCodeDialogContent(
                state = state,
                isLoading = isLoading,
                onCodeChange = viewModel::updateEmailCode,
                onCancel = viewModel::dismissEnterCodeDialog,
                onConfirm = viewModel::confirmEmailChange,
            )
        }
    }
}

@Composable
private fun EmailField(
    email: String,
    onChangeClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.change_email_action),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onChangeClick),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xs)))
        AppOutlinedTextField(
            value = email,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChangeEmailDialogContent(
    state: EditProfileState,
    isLoading: Boolean,
    onNewEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onCancel: () -> Unit,
    onSendCode: () -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.change_email_dialog_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = NocturnePulseTheme.extendedColors.textEmphasis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = stringResource(R.string.change_email_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            AppOutlinedTextField(
                value = state.newEmail,
                onValueChange = onNewEmailChange,
                label = stringResource(R.string.label_new_email),
                placeholder = stringResource(R.string.placeholder_new_email),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))

            AppOutlinedTextField(
                value = state.changePassword,
                onValueChange = onPasswordChange,
                label = stringResource(R.string.label_password),
                error = state.changeEmailError,
                reserveErrorSpace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (state.isChangePasswordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            painter = painterResource(
                                if (state.isChangePasswordVisible) R.drawable.ic_eye_open
                                else R.drawable.ic_eye_closed
                            ),
                            contentDescription = stringResource(R.string.cd_toggle_password),
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_medium)),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

            DialogButtonRow(
                confirmText = stringResource(R.string.action_send_code),
                confirmEnabled = state.canSendChangeCode && !isLoading,
                onCancel = onCancel,
                onConfirm = onSendCode,
                cancelEnabled = !isLoading,
            )
        }

        if (isLoading) DialogLoadingOverlay()
    }
}

@Composable
private fun EnterCodeDialogContent(
    state: EditProfileState,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.change_email_code_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                color = NocturnePulseTheme.extendedColors.textEmphasis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
            Text(
                text = stringResource(R.string.change_email_code_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = state.pendingNewEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.change_email_code_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            AppOtpInput(
                value = state.emailCode,
                onValueChange = onCodeChange,
                length = AuthFlowConstants.EMAIL_CODE_LENGTH,
                isError = state.isEmailCodeError,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.emailCodeError != null) {
                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_s)))
                Text(
                    text = state.emailCodeError,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

            DialogButtonRow(
                confirmText = stringResource(R.string.action_confirm),
                confirmEnabled = state.isEmailCodeComplete && !state.isEmailCodeError && !isLoading,
                onCancel = onCancel,
                onConfirm = onConfirm,
                cancelEnabled = !isLoading,
            )
        }

        if (isLoading) DialogLoadingOverlay()
    }
}

@Composable
private fun DialogButtonRow(
    confirmText: String,
    confirmEnabled: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    cancelEnabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
    ) {
        AppOutlinedButton(
            text = stringResource(R.string.action_cancel),
            onClick = onCancel,
            enabled = cancelEnabled,
            modifier = Modifier.weight(1f),
        )
        AppPrimaryButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BoxScope.DialogLoadingOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AvatarPicker(
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val translucent = NocturnePulseTheme.extendedColors.surfaceTranslucent
    val avatarSize = dimensionResource(R.dimen.size_avatar_large)
    val buttonSize = dimensionResource(R.dimen.size_avatar_button)

    Box(
        modifier = modifier
            .size(avatarSize)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(translucent),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrBlank()) AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .build(),
                contentDescription = stringResource(R.string.cd_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                error = painterResource(R.drawable.ic_user),
            )
            else Icon(
                painter = painterResource(R.drawable.ic_user),
                contentDescription = stringResource(R.string.cd_avatar),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_large)),
            )
        }

        Box(
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(translucent)
                .border(
                    dimensionResource(R.dimen.border_line),
                    MaterialTheme.colorScheme.outline,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(dimensionResource(R.dimen.icon_medium_small)),
            )
        }
    }
}

@Composable
private fun GenderField(
    selected: GenderOption,
    onSelect: (GenderOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        AppOutlinedTextField(
            value = stringResource(selected.labelRes()),
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.label_gender),
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_medium))
                        .rotate(if (expanded) 270f else 90f),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true },
        )

        DropdownMenu(
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            GenderOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PersonalInfoSection(
    state: EditProfileState,
    viewModel: EditProfileViewModel,
) {
    Text(
        text = stringResource(R.string.edit_profile_personal_info),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(R.dimen.spacer_m)),
    )

    BodyGlassCard(
        contentPadding = PaddingValues(
            start = dimensionResource(R.dimen.spacer_xs),
            top = dimensionResource(R.dimen.spacer_l),
            end = dimensionResource(R.dimen.spacer_xs),
            bottom = dimensionResource(R.dimen.spacer_2xs)
        ),
    ) {
        UnitsToggle(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            isMetric = state.isMetric,
            onSelectMetric = viewModel::selectMetric,
            onSelectImperial = viewModel::selectImperial,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_l)))
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_height),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_height_cm,
                state.heightDisplayValue
            )
            else stringResource(
                R.string.unit_height_ft_in,
                state.heightFeet,
                state.heightInches
            ),
            value = state.heightSliderValue,
            valueRange = state.heightSliderRange,
            onValueChange = viewModel::updateHeight,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacer_2xs)))
        MeasurementSlider(
            label = stringResource(R.string.edit_profile_weight),
            valueText = if (state.isMetric) stringResource(
                R.string.unit_weight_kg,
                state.weightDisplayValue
            )
            else stringResource(R.string.unit_weight_lb, state.weightDisplayValue),
            value = state.weightSliderValue,
            valueRange = state.weightSliderRange,
            onValueChange = viewModel::updateWeight,
        )
    }
}

@Composable
private fun UnitsToggle(
    modifier: Modifier,
    isMetric: Boolean,
    onSelectMetric: () -> Unit,
    onSelectImperial: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.edit_profile_units),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        BodySegmentedToggleTrack {
            BodySegmentedToggleChip(
                label = stringResource(R.string.edit_profile_metric),
                isSelected = isMetric,
                onClick = onSelectMetric,
                width = dimensionResource(R.dimen.edit_profile_units_toggle_chip_width),
            )
            Spacer(Modifier.size(dimensionResource(R.dimen.spacer_s)))
            BodySegmentedToggleChip(
                label = stringResource(R.string.edit_profile_imperial),
                isSelected = !isMetric,
                onClick = onSelectImperial,
                width = dimensionResource(R.dimen.edit_profile_units_toggle_chip_width),
            )
        }
    }
}

@Composable
private fun MeasurementSlider(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.spacer_xs)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        AppSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun GenderOption.labelRes(): Int = when (this) {
    GenderOption.Male -> R.string.gender_male
    GenderOption.Female -> R.string.gender_female
    GenderOption.Other -> R.string.gender_other
}
