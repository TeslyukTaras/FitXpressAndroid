package com.hexis.bi.ui.main.buysuit.shipping

import androidx.annotation.StringRes
import com.hexis.bi.R

data class ShippingCountry(
    val name: String,
    val flag: String,
    val dialCode: String,
    val isoCode: String,
)

data class ShippingAddressRules(
    @StringRes val regionLabelRes: Int,
    @StringRes val postalCodeLabelRes: Int,
    val isRegionVisible: Boolean,
    val isRegionRequired: Boolean,
    val isPostalCodeVisible: Boolean,
    val isPostalCodeRequired: Boolean,
)

data class ShippingDetailsState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phoneCountry: ShippingCountry = ShippingCountryProvider.defaultCountry,
    val phoneNumber: String = "",
    val shippingCountry: ShippingCountry = ShippingCountryProvider.defaultCountry,
    val company: String = "",
    val address: String = "",
    val city: String = "",
    val apartment: String = "",
    val region: String = "",
    val postalCode: String = "",
    val note: String = "",
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val phoneNumberError: String? = null,
    val addressError: String? = null,
    val cityError: String? = null,
    val regionError: String? = null,
    val postalCodeError: String? = null,
    val showOrderConfirmation: Boolean = false,
    val showPhoneCountryPicker: Boolean = false,
    val showShippingCountryPicker: Boolean = false,
) {
    val addressRules: ShippingAddressRules
        get() = ShippingAddressRulesProvider.rulesFor(shippingCountry.isoCode)
}

object ShippingAddressRulesProvider {
    private val noPostalCodeCountries = setOf(
        "AE", "AG", "AO", "AW", "BS", "BZ", "BJ", "BW", "BF", "BI", "CM", "CF", "KM",
        "CG", "CD", "CI", "DJ", "DM", "GQ", "ER", "FJ", "GA", "GM", "GH", "GD", "GN",
        "GY", "HK", "IE", "JM", "KE", "KI", "MO", "MW", "ML", "MR", "MU", "NA", "NR",
        "PA", "QA", "RW", "KN", "LC", "ST", "SC", "SL", "SB", "SO", "SR", "SY", "TZ",
        "TL", "TO", "TT", "TV", "UG", "VU", "YE", "ZW",
    )

    private val requiredRegionLabels = mapOf(
        "AR" to R.string.shipping_region_province,
        "AU" to R.string.shipping_region_state_territory,
        "BR" to R.string.shipping_region_state,
        "CA" to R.string.shipping_region_province_territory,
        "CN" to R.string.shipping_region_province,
        "IN" to R.string.shipping_region_state,
        "IT" to R.string.shipping_region_province,
        "JP" to R.string.shipping_region_prefecture,
        "MX" to R.string.shipping_region_state,
        "MY" to R.string.shipping_region_state,
        "NZ" to R.string.shipping_region_region,
        "TH" to R.string.shipping_region_province,
        "US" to R.string.shipping_region_state,
        "ZA" to R.string.shipping_region_province,
    )

    private val optionalRegionLabels = mapOf(
        "AE" to R.string.shipping_region_emirate,
        "ES" to R.string.shipping_region_province,
        "FR" to R.string.shipping_region_department,
        "GB" to R.string.shipping_region_county,
        "ID" to R.string.shipping_region_province,
        "NL" to R.string.shipping_region_province,
        "PH" to R.string.shipping_region_province,
        "PL" to R.string.shipping_region_voivodeship,
        "TR" to R.string.shipping_region_province,
        "UA" to R.string.shipping_region_region,
    )

    fun rulesFor(isoCode: String): ShippingAddressRules {
        val upperIso = isoCode.uppercase()
        val requiredRegionLabelRes = requiredRegionLabels[upperIso]
        val optionalRegionLabelRes = optionalRegionLabels[upperIso]
        val hasPostalCode = upperIso !in noPostalCodeCountries

        return ShippingAddressRules(
            regionLabelRes = requiredRegionLabelRes ?: optionalRegionLabelRes ?: R.string.shipping_region_generic,
            postalCodeLabelRes = when (upperIso) {
                "US" -> R.string.shipping_postal_zip
                "GB" -> R.string.shipping_postal_postcode
                else -> R.string.shipping_postal_generic
            },
            isRegionVisible = requiredRegionLabelRes != null || optionalRegionLabelRes != null,
            isRegionRequired = requiredRegionLabelRes != null,
            isPostalCodeVisible = hasPostalCode,
            isPostalCodeRequired = hasPostalCode,
        )
    }
}
