package com.hexis.bi.ui.main.buysuit.shipping

import android.content.Context
import com.hexis.bi.R
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.utils.constants.ShippingConstants

interface AddressFormFields {
    val shippingCountry: ShippingCountry
    val company: String
    val address: String
    val city: String
    val apartment: String
    val region: String
    val postalCode: String
    val note: String
    val addressRules: ShippingAddressRules
}

/** Validation result for the address fields; `null` per field means "no error". */
data class AddressErrors(
    val addressError: String?,
    val cityError: String?,
    val regionError: String?,
    val postalCodeError: String?,
) {
    val hasError: Boolean
        get() = listOf(addressError, cityError, regionError, postalCodeError).any { it != null }
}

/** Validates the address fields against the selected country's [ShippingAddressRules]. */
fun AddressFormFields.validateAddress(context: Context): AddressErrors {
    val rules = addressRules
    return AddressErrors(
        addressError = if (address.isBlank()) context.getString(R.string.error_address_required) else null,
        cityError = if (city.isBlank()) context.getString(R.string.error_city_required) else null,
        regionError = when {
            rules.isRegionRequired && region.isBlank() ->
                context.getString(R.string.error_field_required, context.getString(rules.regionLabelRes))
            else -> null
        },
        postalCodeError = when {
            rules.isPostalCodeRequired && postalCode.isBlank() ->
                context.getString(R.string.error_field_required, context.getString(rules.postalCodeLabelRes))
            rules.isPostalCodeVisible && postalCode.isNotBlank() &&
                    postalCode.trim().length < ShippingConstants.MIN_POSTAL_CODE_LENGTH ->
                context.getString(R.string.error_postal_code_invalid)
            else -> null
        },
    )
}

/** Maps the (trimmed) address fields into the domain [OrderShippingAddress]. */
fun AddressFormFields.toOrderShippingAddress() = OrderShippingAddress(
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
