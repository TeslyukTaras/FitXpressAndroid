package com.hexis.bi.ui.main.buysuit.editaddress

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hexis.bi.R
import com.hexis.bi.ui.base.BaseScreen
import com.hexis.bi.ui.base.BaseTopBar
import com.hexis.bi.ui.components.AppPrimaryButton
import com.hexis.bi.ui.main.buysuit.shipping.CountryPickerField
import com.hexis.bi.ui.main.buysuit.shipping.CountryPickerSheet
import com.hexis.bi.ui.main.buysuit.shipping.ShippingCountryProvider
import com.hexis.bi.ui.main.buysuit.shipping.ShippingField
import com.hexis.bi.ui.main.buysuit.shipping.requiredLabel
import com.hexis.bi.ui.theme.screenBackground
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun EditAddressScreen(
    orderId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAddressViewModel = koinViewModel { parametersOf(orderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(state.submitted) {
        if (state.submitted) onSaved()
    }

    Box(modifier = modifier) {
        BaseScreen(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (state.showCountryPicker) {
                        Modifier.blur(dimensionResource(R.dimen.blur_dialog_backdrop))
                    } else {
                        Modifier
                    }
                )
                .screenBackground(),
            containerColor = Color.Transparent,
            isLoading = isLoading,
            error = error,
            onDismissError = viewModel::clearError,
            topBar = {
                BaseTopBar(
                    title = stringResource(R.string.edit_address_title),
                    background = Color.Transparent,
                    onBack = onBack,
                )
            },
        ) {
            EditAddressContent(state = state, viewModel = viewModel)
        }

        if (state.showCountryPicker) {
            CountryPickerSheet(
                countries = ShippingCountryProvider.countries,
                selectedCountry = state.shippingCountry,
                showDialCode = false,
                onCountrySelected = viewModel::updateShippingCountry,
                onDismiss = viewModel::dismissCountryPicker,
            )
        }
    }
}

@Composable
private fun EditAddressContent(
    state: EditAddressState,
    viewModel: EditAddressViewModel,
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

        CountryPickerField(
            country = state.shippingCountry,
            label = stringResource(R.string.shipping_country_region),
            onClick = viewModel::showCountryPicker,
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

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_m)))

        Text(
            text = stringResource(R.string.edit_address_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_2xl)))

        AppPrimaryButton(
            text = stringResource(R.string.edit_address_save),
            onClick = viewModel::submit,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(dimensionResource(R.dimen.spacer_l)))
    }
}
