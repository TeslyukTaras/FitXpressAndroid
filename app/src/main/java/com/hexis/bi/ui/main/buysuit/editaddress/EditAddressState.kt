package com.hexis.bi.ui.main.buysuit.editaddress

import com.hexis.bi.ui.main.buysuit.shipping.AddressFormFields
import com.hexis.bi.ui.main.buysuit.shipping.ShippingAddressRules
import com.hexis.bi.ui.main.buysuit.shipping.ShippingAddressRulesProvider
import com.hexis.bi.ui.main.buysuit.shipping.ShippingCountry
import com.hexis.bi.ui.main.buysuit.shipping.ShippingCountryProvider

/**
 * Address-only form for requesting a shipping-address change on an existing order. Mirrors the
 * address section of [com.hexis.bi.ui.main.buysuit.shipping.ShippingDetailsState] (no contact fields).
 */
data class EditAddressState(
    override val shippingCountry: ShippingCountry = ShippingCountryProvider.defaultCountry,
    override val company: String = "",
    override val address: String = "",
    override val city: String = "",
    override val apartment: String = "",
    override val region: String = "",
    override val postalCode: String = "",
    override val note: String = "",
    val addressError: String? = null,
    val cityError: String? = null,
    val regionError: String? = null,
    val postalCodeError: String? = null,
    val showCountryPicker: Boolean = false,
    /** Flips true once the change request is submitted, so the screen can navigate back. */
    val submitted: Boolean = false,
) : AddressFormFields {
    override val addressRules: ShippingAddressRules
        get() = ShippingAddressRulesProvider.rulesFor(shippingCountry.isoCode)
}
