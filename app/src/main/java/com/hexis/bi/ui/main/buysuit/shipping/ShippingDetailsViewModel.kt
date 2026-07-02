package com.hexis.bi.ui.main.buysuit.shipping

import android.app.Application
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.hexis.bi.R
import com.hexis.bi.data.order.OrderDraftHolder
import com.hexis.bi.data.user.UserRepository
import com.hexis.bi.domain.order.OrderContact
import com.hexis.bi.domain.order.OrderDraft
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.order.OrderSizing
import com.hexis.bi.ui.base.BaseViewModel
import com.hexis.bi.utils.isValidEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShippingDetailsViewModel(
    application: Application,
    private val userRepository: UserRepository,
    private val orderRepository: OrderRepository,
    private val orderDraftHolder: OrderDraftHolder,
) : BaseViewModel(application) {

    private val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    private val _state = MutableStateFlow(ShippingDetailsState())
    val state: StateFlow<ShippingDetailsState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() = launch(showLoading = false) {
        userRepository.getUser().onSuccess { profile ->
            _state.update {
                it.copy(
                    firstName = profile.firstName,
                    lastName = profile.lastName,
                    email = profile.email,
                )
            }
        }
    }

    fun updateFirstName(value: String) = _state.update { it.copy(firstName = value, firstNameError = null) }
    fun updateLastName(value: String) = _state.update { it.copy(lastName = value, lastNameError = null) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value, emailError = null) }

    fun updatePhoneCountry(value: ShippingCountry) =
        _state.update {
            it.copy(
                phoneCountry = value,
                phoneNumberError = null,
                showPhoneCountryPicker = false,
            )
        }

    fun updateShippingCountry(value: ShippingCountry) =
        _state.update {
            if (value.isoCode == it.shippingCountry.isoCode) {
                it.copy(showShippingCountryPicker = false)
            } else {
                // Region and postal code are country-specific; street and city carry over.
                it.copy(
                    shippingCountry = value,
                    region = "",
                    regionError = null,
                    postalCode = "",
                    postalCodeError = null,
                    showShippingCountryPicker = false,
                )
            }
        }

    fun updatePhoneNumber(value: String) = _state.update { it.copy(phoneNumber = value, phoneNumberError = null) }
    fun updateCompany(value: String) = _state.update { it.copy(company = value) }
    fun updateAddress(value: String) = _state.update { it.copy(address = value, addressError = null) }
    fun updateCity(value: String) = _state.update { it.copy(city = value, cityError = null) }
    fun updateApartment(value: String) = _state.update { it.copy(apartment = value) }
    fun updateRegion(value: String) = _state.update { it.copy(region = value, regionError = null) }
    fun updatePostalCode(value: String) = _state.update { it.copy(postalCode = value, postalCodeError = null) }
    fun updateNote(value: String) = _state.update { it.copy(note = value) }

    fun showPhoneCountryPicker() =
        _state.update { it.copy(showPhoneCountryPicker = true) }

    fun dismissPhoneCountryPicker() =
        _state.update { it.copy(showPhoneCountryPicker = false) }

    fun showShippingCountryPicker() =
        _state.update { it.copy(showShippingCountryPicker = true) }

    fun dismissShippingCountryPicker() =
        _state.update { it.copy(showShippingCountryPicker = false) }

    fun placeOrder() {
        if (!validate()) return
        val sizing = orderDraftHolder.sizing
        if (sizing == null) {
            setError(R.string.error_place_order_failed)
            return
        }
        launch(onError = { setError(R.string.error_place_order_failed) }) {
            orderRepository.placeOrder(_state.value.toOrderDraft(sizing)).getOrThrow()
            orderDraftHolder.sizing = null
            _state.update { it.copy(showOrderConfirmation = true) }
        }
    }

    fun dismissOrderConfirmation() =
        _state.update { it.copy(showOrderConfirmation = false) }

    private fun validate(): Boolean {
        val s = _state.value
        val firstNameError = if (s.firstName.isBlank()) appContext.getString(R.string.error_first_name_required) else null
        val lastNameError = if (s.lastName.isBlank()) appContext.getString(R.string.error_last_name_required) else null
        val emailError = when {
            s.email.isBlank() -> appContext.getString(R.string.error_email_required)
            !s.email.isValidEmail() -> appContext.getString(R.string.error_email_invalid)
            else -> null
        }
        val phoneNumberError = when {
            s.phoneNumber.isBlank() -> appContext.getString(R.string.error_phone_required)
            !isValidPhoneNumber(s.phoneNumber, s.phoneCountry.isoCode) -> appContext.getString(R.string.error_phone_invalid)
            else -> null
        }
        val addressErrors = s.validateAddress(appContext)

        val hasError = listOf(
            firstNameError,
            lastNameError,
            emailError,
            phoneNumberError,
            addressErrors.addressError,
            addressErrors.cityError,
            addressErrors.regionError,
            addressErrors.postalCodeError,
        ).any { it != null }

        _state.update {
            it.copy(
                firstNameError = firstNameError,
                lastNameError = lastNameError,
                emailError = emailError,
                phoneNumberError = phoneNumberError,
                addressError = addressErrors.addressError,
                cityError = addressErrors.cityError,
                regionError = addressErrors.regionError,
                postalCodeError = addressErrors.postalCodeError,
            )
        }

        return !hasError
    }

    private fun ShippingDetailsState.toOrderDraft(sizing: OrderSizing) = OrderDraft(
        contact = OrderContact(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim(),
            phoneCountryIso = phoneCountry.isoCode,
            phoneDialCode = phoneCountry.dialCode,
            phoneNumber = phoneNumber.trim(),
        ),
        shippingAddress = toOrderShippingAddress(),
        sizing = sizing,
    )

    private fun isValidPhoneNumber(phoneNumber: String, isoCode: String): Boolean =
        try {
            val parsed = phoneNumberUtil.parse(phoneNumber, isoCode)
            phoneNumberUtil.isValidNumberForRegion(parsed, isoCode)
        } catch (_: NumberParseException) {
            false
        }
}
