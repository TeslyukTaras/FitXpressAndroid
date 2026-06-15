package com.hexis.bi.ui.main.buysuit.editaddress

import android.app.Application
import com.hexis.bi.R
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.ui.main.buysuit.shipping.ShippingCountry
import com.hexis.bi.ui.main.buysuit.shipping.ShippingCountryProvider
import com.hexis.bi.utils.constants.ShippingConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditAddressViewModel(
    application: Application,
    private val orderRepository: OrderRepository,
    private val orderId: String,
) : BaseViewModel(application) {

    private val _state = MutableStateFlow(EditAddressState())
    val state: StateFlow<EditAddressState> = _state.asStateFlow()

    init {
        loadOrderAddress()
    }

    /** Pre-fills with the order's current effective address (the pending request, if one exists). */
    private fun loadOrderAddress() = launch {
        val order = orderRepository.getOrderById(orderId).getOrThrow()
            ?: error("Order $orderId not found")
        val address = order.effectiveShippingAddress
        _state.update {
            it.copy(
                shippingCountry = ShippingCountryProvider.countryForIso(address.countryIso),
                company = address.company,
                address = address.addressLine,
                city = address.city,
                apartment = address.apartment,
                region = address.region,
                postalCode = address.postalCode,
                note = address.note,
            )
        }
    }

    fun updateShippingCountry(value: ShippingCountry) =
        _state.update {
            if (value.isoCode == it.shippingCountry.isoCode) {
                it.copy(showCountryPicker = false)
            } else {
                // Region and postal code are country-specific; street and city carry over.
                it.copy(
                    shippingCountry = value,
                    region = "",
                    regionError = null,
                    postalCode = "",
                    postalCodeError = null,
                    showCountryPicker = false,
                )
            }
        }

    fun updateCompany(value: String) = _state.update { it.copy(company = value) }
    fun updateAddress(value: String) = _state.update { it.copy(address = value, addressError = null) }
    fun updateCity(value: String) = _state.update { it.copy(city = value, cityError = null) }
    fun updateApartment(value: String) = _state.update { it.copy(apartment = value) }
    fun updateRegion(value: String) = _state.update { it.copy(region = value, regionError = null) }
    fun updatePostalCode(value: String) = _state.update { it.copy(postalCode = value, postalCodeError = null) }
    fun updateNote(value: String) = _state.update { it.copy(note = value) }

    fun showCountryPicker() = _state.update { it.copy(showCountryPicker = true) }
    fun dismissCountryPicker() = _state.update { it.copy(showCountryPicker = false) }

    fun submit() {
        if (!validate()) return
        launch(onError = { setError(R.string.error_address_change_failed) }) {
            orderRepository.requestAddressChange(orderId, _state.value.toAddress()).getOrThrow()
            _state.update { it.copy(submitted = true) }
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        val addressError = if (s.address.isBlank()) appContext.getString(R.string.error_address_required) else null
        val cityError = if (s.city.isBlank()) appContext.getString(R.string.error_city_required) else null
        val rules = s.addressRules
        val regionError = when {
            rules.isRegionRequired && s.region.isBlank() ->
                appContext.getString(R.string.error_field_required, appContext.getString(rules.regionLabelRes))
            else -> null
        }
        val postalCodeError = when {
            rules.isPostalCodeRequired && s.postalCode.isBlank() ->
                appContext.getString(R.string.error_field_required, appContext.getString(rules.postalCodeLabelRes))
            rules.isPostalCodeVisible && s.postalCode.isNotBlank() &&
                    s.postalCode.trim().length < ShippingConstants.MIN_POSTAL_CODE_LENGTH ->
                appContext.getString(R.string.error_postal_code_invalid)
            else -> null
        }

        val hasError = listOf(addressError, cityError, regionError, postalCodeError).any { it != null }
        _state.update {
            it.copy(
                addressError = addressError,
                cityError = cityError,
                regionError = regionError,
                postalCodeError = postalCodeError,
            )
        }
        return !hasError
    }

    private fun EditAddressState.toAddress() = OrderShippingAddress(
        countryIso = shippingCountry.isoCode,
        countryName = shippingCountry.name,
        company = company.trim(),
        addressLine = address.trim(),
        apartment = apartment.trim(),
        city = city.trim(),
        region = region.trim(),
        postalCode = postalCode.trim(),
        note = note.trim(),
    )
}
