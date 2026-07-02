package com.hexis.bi.data.order

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.hexis.bi.domain.order.Order
import com.hexis.bi.domain.order.OrderAddressChangeRequest
import com.hexis.bi.domain.order.OrderAddressChangeStatus
import com.hexis.bi.domain.order.OrderContact
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.domain.order.OrderSizing
import com.hexis.bi.domain.order.OrderStatus
import com.hexis.bi.domain.order.OrderStatusEvent
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_ADDRESS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_REQUEST
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_REQUESTED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_LINE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_APARTMENT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CARRIER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CITY
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_COMPANY
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CONTACT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_COUNTRY_ISO
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_COUNTRY_NAME
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CREATED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_EMAIL
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_FIRST_NAME
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_HEIGHT_CM
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_LAST_NAME
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_NOTE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ORDER_NUMBER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_PHONE_COUNTRY_ISO
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_PHONE_DIAL_CODE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_PHONE_NUMBER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_POSTAL_CODE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_REGION
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SCAN_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SHIPPING_ADDRESS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_ESTIMATED
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_HISTORY
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_SIZE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_TRACKING_NUMBER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_WEIGHT_KG

object OrderFirestoreMapper {

    fun toMap(contact: OrderContact): Map<String, String> = mapOf(
        FIELD_FIRST_NAME to contact.firstName,
        FIELD_LAST_NAME to contact.lastName,
        FIELD_EMAIL to contact.email,
        FIELD_PHONE_COUNTRY_ISO to contact.phoneCountryIso,
        FIELD_PHONE_DIAL_CODE to contact.phoneDialCode,
        FIELD_PHONE_NUMBER to contact.phoneNumber,
    )

    fun toMap(address: OrderShippingAddress): Map<String, String> = mapOf(
        FIELD_COUNTRY_ISO to address.countryIso,
        FIELD_COUNTRY_NAME to address.countryName,
        FIELD_COMPANY to address.company,
        FIELD_ADDRESS_LINE to address.addressLine,
        FIELD_APARTMENT to address.apartment,
        FIELD_CITY to address.city,
        FIELD_REGION to address.region,
        FIELD_POSTAL_CODE to address.postalCode,
        FIELD_NOTE to address.note,
    )

    fun toOrder(snapshot: DocumentSnapshot): Order? = with(snapshot) {
        val status = enumOrNull<OrderStatus>(getString(FIELD_STATUS)) ?: return null
        Order(
            id = id,
            orderNumber = getString(FIELD_ORDER_NUMBER) ?: return null,
            status = status,
            statusHistory = parseStatusHistory(),
            createdAtMillis = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
            trackingNumber = getString(FIELD_TRACKING_NUMBER),
            carrier = getString(FIELD_CARRIER),
            suitId = getString(FIELD_SUIT_ID),
            sizing = OrderSizing(
                scanId = getString(FIELD_SCAN_ID),
                suitSize = getString(FIELD_SUIT_SIZE).orEmpty(),
                heightCm = (get(FIELD_HEIGHT_CM) as? Number)?.toFloat() ?: 0f,
                weightKg = (get(FIELD_WEIGHT_KG) as? Number)?.toFloat() ?: 0f,
            ),
            contact = parseContact(),
            shippingAddress = (get(FIELD_SHIPPING_ADDRESS) as? Map<*, *>).toShippingAddress(),
            addressChangeRequest = parseAddressChangeRequest(),
        )
    }

    private fun DocumentSnapshot.parseStatusHistory(): List<OrderStatusEvent> {
        val raw = get(FIELD_STATUS_HISTORY) as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val status = enumOrNull<OrderStatus>(map[FIELD_STATUS_EVENT_STATUS] as? String) ?: return@mapNotNull null
            OrderStatusEvent(
                status = status,
                estimatedAtMillis = (map[FIELD_STATUS_EVENT_ESTIMATED] as? Timestamp)?.toDate()?.time,
                atMillis = (map[FIELD_STATUS_EVENT_AT] as? Timestamp)?.toDate()?.time,
            )
        }
    }

    private fun DocumentSnapshot.parseContact(): OrderContact {
        val map = get(FIELD_CONTACT) as? Map<*, *> ?: emptyMap<Any, Any>()
        return OrderContact(
            firstName = map.stringValue(FIELD_FIRST_NAME),
            lastName = map.stringValue(FIELD_LAST_NAME),
            email = map.stringValue(FIELD_EMAIL),
            phoneCountryIso = map.stringValue(FIELD_PHONE_COUNTRY_ISO),
            phoneDialCode = map.stringValue(FIELD_PHONE_DIAL_CODE),
            phoneNumber = map.stringValue(FIELD_PHONE_NUMBER),
        )
    }

    private fun DocumentSnapshot.parseAddressChangeRequest(): OrderAddressChangeRequest? {
        val map = get(FIELD_ADDRESS_CHANGE_REQUEST) as? Map<*, *> ?: return null
        val status = enumOrNull<OrderAddressChangeStatus>(map[FIELD_ADDRESS_CHANGE_STATUS] as? String) ?: return null
        return OrderAddressChangeRequest(
            address = (map[FIELD_ADDRESS_CHANGE_ADDRESS] as? Map<*, *>).toShippingAddress(),
            requestedAtMillis = (map[FIELD_ADDRESS_CHANGE_REQUESTED_AT] as? Timestamp)?.toDate()?.time ?: 0L,
            status = status,
        )
    }

    private fun Map<*, *>?.toShippingAddress(): OrderShippingAddress {
        val map = this ?: emptyMap<Any, Any>()
        return OrderShippingAddress(
            countryIso = map.stringValue(FIELD_COUNTRY_ISO),
            countryName = map.stringValue(FIELD_COUNTRY_NAME),
            company = map.stringValue(FIELD_COMPANY),
            addressLine = map.stringValue(FIELD_ADDRESS_LINE),
            apartment = map.stringValue(FIELD_APARTMENT),
            city = map.stringValue(FIELD_CITY),
            region = map.stringValue(FIELD_REGION),
            postalCode = map.stringValue(FIELD_POSTAL_CODE),
            note = map.stringValue(FIELD_NOTE),
        )
    }

    private fun Map<*, *>.stringValue(key: String): String = this[key] as? String ?: ""

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        enumValues<T>().firstOrNull { it.name == name }
}
