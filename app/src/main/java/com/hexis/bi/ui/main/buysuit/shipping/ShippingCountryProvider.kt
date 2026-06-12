package com.hexis.bi.ui.main.buysuit.shipping

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

object ShippingCountryProvider {
    private val phoneNumberUtil: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    val countries: List<ShippingCountry> by lazy {
        phoneNumberUtil.supportedRegions
            .mapNotNull { iso ->
                val dialCode = phoneNumberUtil.getCountryCodeForRegion(iso)
                    .takeIf { it > 0 }
                    ?: return@mapNotNull null
                val name = Locale.Builder()
                    .setRegion(iso)
                    .build()
                    .getDisplayCountry(Locale.getDefault())
                    .takeIf { it.isNotBlank() }
                    ?: iso
                ShippingCountry(
                    name = name,
                    flag = iso.toFlagEmoji(),
                    dialCode = "+$dialCode",
                    isoCode = iso,
                )
            }
            .sortedBy { it.name }
    }

    val defaultCountry: ShippingCountry by lazy {
        val deviceIso = Locale.getDefault().country
        countries.firstOrNull { it.isoCode.equals(deviceIso, ignoreCase = true) }
            ?: countries.firstOrNull { it.isoCode == FALLBACK_ISO_CODE }
            ?: countries.first()
    }

    fun examplePhoneNumber(isoCode: String): String? =
        phoneNumberUtil.getExampleNumberForType(isoCode, PhoneNumberUtil.PhoneNumberType.MOBILE)
            ?.let { phoneNumberUtil.format(it, PhoneNumberUtil.PhoneNumberFormat.NATIONAL) }

    private fun String.toFlagEmoji(): String {
        val upper = uppercase(Locale.US)
        if (upper.length != 2) return upper
        val first = Character.codePointAt(upper, 0) - 'A'.code + REGIONAL_INDICATOR_A
        val second = Character.codePointAt(upper, 1) - 'A'.code + REGIONAL_INDICATOR_A
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    private const val FALLBACK_ISO_CODE = "UA"
    private const val REGIONAL_INDICATOR_A = 0x1F1E6
}
