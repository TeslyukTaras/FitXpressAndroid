package com.hexis.bi.ui.main.buysuit.shipping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppDialog
import com.hexis.bi.ui.dark.DarkOutlinedTextField
import com.hexis.bi.ui.dark.DarkPrimaryButton
import com.hexis.bi.ui.dark.darkScreenBackground
import com.hexis.bi.ui.theme.dark.DarkTheme
import com.hexis.bi.utils.constants.GlassConstants
import com.hexis.bi.utils.glass
import org.koin.androidx.compose.koinViewModel

@Composable
fun ShippingDetailsScreen(
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShippingDetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val showOverlay = state.showOrderConfirmation ||
            state.showPhoneCountryPicker ||
            state.showShippingCountryPicker

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showOverlay) {
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    } else {
                        Modifier
                    }
                )
                .darkScreenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.shipping_details_title),
                    background = Color.Transparent,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onClose) {
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
            ShippingDetailsContent(
                state = state,
                viewModel = viewModel,
            )
        }

        if (state.showOrderConfirmation) {
            // Order is already placed at this point, so every dismiss path (Ok, X,
            // backdrop) must leave the form — staying would invite a duplicate order.
            val closeConfirmation = {
                viewModel.dismissOrderConfirmation()
                onClose()
            }
            AppDialog(onDismiss = closeConfirmation) {
                OrderConfirmationDialog(onDismiss = closeConfirmation)
            }
        }

        if (state.showPhoneCountryPicker) {
            CountryPickerSheet(
                countries = ShippingCountryProvider.countries,
                selectedCountry = state.phoneCountry,
                showDialCode = true,
                onCountrySelected = viewModel::updatePhoneCountry,
                onDismiss = viewModel::dismissPhoneCountryPicker,
            )
        }

        if (state.showShippingCountryPicker) {
            CountryPickerSheet(
                countries = ShippingCountryProvider.countries,
                selectedCountry = state.shippingCountry,
                showDialCode = false,
                onCountrySelected = viewModel::updateShippingCountry,
                onDismiss = viewModel::dismissShippingCountryPicker,
            )
        }
    }
}

@Composable
private fun ShippingDetailsContent(
    state: ShippingDetailsState,
    viewModel: ShippingDetailsViewModel,
) {
    val addressRules = state.addressRules

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = dimensionResource(R.dimen.padding_medium))
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacer_m)),
        ) {
            DarkOutlinedTextField(
                value = state.firstName,
                onValueChange = viewModel::updateFirstName,
                label = stringResource(R.string.label_first_name),
                error = state.firstNameError,
                modifier = Modifier.weight(1f),
            )
            DarkOutlinedTextField(
                value = state.lastName,
                onValueChange = viewModel::updateLastName,
                label = stringResource(R.string.label_last_name),
                error = state.lastNameError,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        DarkOutlinedTextField(
            value = state.email,
            onValueChange = viewModel::updateEmail,
            label = stringResource(R.string.label_email),
            error = state.emailError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        PhoneNumberField(
            country = state.phoneCountry,
            onCountryClick = viewModel::showPhoneCountryPicker,
            phoneNumber = state.phoneNumber,
            onPhoneNumberChange = viewModel::updatePhoneNumber,
            error = state.phoneNumberError,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_xl)))

        Text(
            text = stringResource(R.string.shipping_address_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))

        CountryPickerField(
            country = state.shippingCountry,
            label = stringResource(R.string.shipping_country_region),
            onClick = viewModel::showShippingCountryPicker,
        )
        ShippingField(
            value = state.company,
            onValueChange = viewModel::updateCompany,
            label = requiredLabel(stringResource(R.string.shipping_company), isRequired = false),
        )
        ShippingField(
            value = state.address,
            onValueChange = viewModel::updateAddress,
            label = stringResource(R.string.shipping_address),
            error = state.addressError,
        )
        ShippingField(
            value = state.city,
            onValueChange = viewModel::updateCity,
            label = stringResource(R.string.shipping_city),
            error = state.cityError,
        )
        ShippingField(
            value = state.apartment,
            onValueChange = viewModel::updateApartment,
            label = requiredLabel(stringResource(R.string.shipping_apartment), isRequired = false),
        )
        if (addressRules.isRegionVisible) {
            ShippingField(
                value = state.region,
                onValueChange = viewModel::updateRegion,
                label = requiredLabel(
                    stringResource(addressRules.regionLabelRes),
                    addressRules.isRegionRequired,
                ),
                error = state.regionError,
            )
        }
        if (addressRules.isPostalCodeVisible) {
            ShippingField(
                value = state.postalCode,
                onValueChange = viewModel::updatePostalCode,
                label = requiredLabel(
                    stringResource(addressRules.postalCodeLabelRes),
                    addressRules.isPostalCodeRequired,
                ),
                error = state.postalCodeError,
            )
        }
        ShippingField(
            value = state.note,
            onValueChange = viewModel::updateNote,
            label = requiredLabel(stringResource(R.string.shipping_note), isRequired = false),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        DarkPrimaryButton(
            text = stringResource(R.string.shipping_place_order),
            onClick = viewModel::placeOrder,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}

@Composable
private fun PhoneNumberField(
    country: ShippingCountry,
    onCountryClick: () -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    error: String?,
) {
    val borderColor =
        if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
    val interactionSource = remember { MutableInteractionSource() }
    val placeholder = remember(country.isoCode) {
        ShippingCountryProvider.examplePhoneNumber(country.isoCode)
    } ?: stringResource(R.string.shipping_phone_placeholder)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.shipping_phone_number),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacer_2xs)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .glass(
                    shape = MaterialTheme.shapes.small,
                    level = GlassConstants.LEVEL_DEFAULT,
                    fill = DarkTheme.extendedColors.surfaceTranslucent,
                    backgroundBlur = dimensionResource(R.dimen.glass_background_blur),
                    rimWidth = dimensionResource(R.dimen.glass_rim_width),
                    backgroundAlpha = GlassConstants.TEXT_FIELD_BACKGROUND_ALPHA,
                )
                .border(
                    dimensionResource(R.dimen.border_line),
                    borderColor.copy(alpha = GlassConstants.TEXT_FIELD_BORDER_ALPHA),
                    MaterialTheme.shapes.small,
                )
                .padding(dimensionResource(R.dimen.spacer_m)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onCountryClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountryFlagCircle(
                    country = country,
                    size = dimensionResource(R.dimen.flag_circle_small)
                )
                Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xxs)))
                Icon(
                    painter = painterResource(R.drawable.ic_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(dimensionResource(R.dimen.icon_small))
                        .rotate(90f),
                )
            }

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_m)))

            Text(
                text = country.dialCode,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.width(dimensionResource(R.dimen.spacer_xs)))

            BasicTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = MaterialTheme.typography.labelLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    TextFieldDefaults.DecorationBox(
                        value = phoneNumber,
                        innerTextField = innerTextField,
                        enabled = true,
                        singleLine = true,
                        visualTransformation = VisualTransformation.None,
                        interactionSource = interactionSource,
                        placeholder = {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        },
                        contentPadding = PaddingValues(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                    )
                },
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacer_2xs)),
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))
    }
}

@Composable
private fun OrderConfirmationDialog(
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(R.dimen.padding_large),
                horizontal = dimensionResource(R.dimen.padding_medium),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val textPadding = Modifier.padding(horizontal = dimensionResource(R.dimen.spacer_m))

        Text(
            text = stringResource(R.string.shipping_thanks_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = textPadding,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = stringResource(R.string.shipping_thanks_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = textPadding,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = stringResource(R.string.shipping_thanks_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = textPadding,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        DarkPrimaryButton(
            text = stringResource(R.string.action_ok),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
