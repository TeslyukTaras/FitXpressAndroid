package com.hexis.bi.ui.main.settings.editprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.domain.enums.GenderOption
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppAvatar
import com.hexis.bi.ui.components.AppButton
import com.hexis.bi.ui.components.AppDatePicker
import com.hexis.bi.ui.components.AppDropdown
import com.hexis.bi.ui.components.AppTextField
import com.hexis.bi.ui.components.profile.HeathParametersSection
import com.hexis.bi.utils.parseDob
import org.koin.androidx.compose.koinViewModel

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

    val datePickerState = rememberDatePickerState()
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
                    if (state.showDatePicker)
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    else Modifier
                ),
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            message = message,
            onDismissMessage = viewModel::clearMessage,
            onInitialization = viewModel::loadUser,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.edit_profile_title),
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
                    avatarUrl = state.avatarUrl,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_xs)),
                ) {
                    AppTextField(
                        value = state.firstName,
                        onValueChange = viewModel::updateFirstName,
                        label = stringResource(R.string.label_first_name),
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = state.lastName,
                        onValueChange = viewModel::updateLastName,
                        label = stringResource(R.string.label_last_name),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                AppTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = stringResource(R.string.label_email),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

                Box(modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = state.dateOfBirth,
                        onValueChange = {},
                        readOnly = true,
                        label = stringResource(R.string.label_date_of_birth),
                        placeholder = stringResource(R.string.placeholder_date_of_birth),
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
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

                AppDropdown(
                    options = GenderOption.entries,
                    selectedOption = state.gender,
                    onOptionSelected = { viewModel.selectGender(it) },
                    label = stringResource(R.string.label_gender),
                    optionLabel = { option ->
                        stringResource(
                            when (option) {
                                GenderOption.Male -> R.string.gender_male
                                GenderOption.Female -> R.string.gender_female
                                GenderOption.Other -> R.string.gender_other
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

                PersonalInfoSection(state = state, viewModel = viewModel)

                Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

                AppButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
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
    }
}

@Composable
private fun AvatarPicker(
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(dimensionResource(R.dimen.size_avatar_large))
            .clickable(onClick = onClick),
    ) {
        AppAvatar(
            imageUrl = avatarUrl,
            size = dimensionResource(R.dimen.size_avatar_large),
        )
        Box(
            modifier = Modifier
                .size(dimensionResource(R.dimen.size_avatar_button))
                .align(Alignment.BottomEnd)
                .shadow(
                    elevation = dimensionResource(R.dimen.elevation_box),
                    shape = CircleShape,
                    ambientColor = MaterialTheme.colorScheme.outlineVariant,
                )
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
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
private fun PersonalInfoSection(
    state: EditProfileState,
    viewModel: EditProfileViewModel,
) {
    Text(
        text = stringResource(R.string.edit_profile_personal_info),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

    HeathParametersSection(
        params = state,
        onSelectMetric = viewModel::selectMetric,
        onSelectImperial = viewModel::selectImperial,
        onHeightChange = viewModel::updateHeight,
        onWeightChange = viewModel::updateWeight,
    )
}
