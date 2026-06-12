package com.hexis.bi.data.order

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hexis.bi.domain.order.Order
import com.hexis.bi.domain.order.OrderContact
import com.hexis.bi.domain.order.OrderDraft
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.domain.order.OrderSizing
import com.hexis.bi.domain.order.OrderStatus
import com.hexis.bi.domain.order.OrderStatusEvent
import com.hexis.bi.utils.constants.OrderFirestoreConstants.COLLECTION_ORDERS
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
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ETA
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
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_HISTORY
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_SIZE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_TRACKING_NUMBER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_UPDATED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_WEIGHT_KG
import com.hexis.bi.utils.constants.OrderFirestoreConstants.LATEST_ORDER_LOOKBACK
import com.hexis.bi.utils.constants.OrderFirestoreConstants.ORDER_NUMBER_ID_LENGTH
import com.hexis.bi.utils.constants.OrderFirestoreConstants.ORDER_NUMBER_PREFIX
import com.hexis.bi.utils.constants.ScanFirestoreConstants.COLLECTION_USERS
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale

class FirestoreOrderRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : OrderRepository {

    private fun ordersCollection() =
        firestore.collection(COLLECTION_USERS)
            .document(auth.currentUser?.uid ?: error("Not authenticated"))
            .collection(COLLECTION_ORDERS)

    override suspend fun placeOrder(draft: OrderDraft): Result<Order> = runCatching {
        val docRef = ordersCollection().document()
        val now = Timestamp.now()
        val orderNumber = ORDER_NUMBER_PREFIX +
                docRef.id.take(ORDER_NUMBER_ID_LENGTH).uppercase(Locale.US)

        val data = buildMap<String, Any?> {
            put(FIELD_ORDER_NUMBER, orderNumber)
            put(FIELD_STATUS, OrderStatus.PLACED.name)
            put(
                FIELD_STATUS_HISTORY,
                listOf(
                    mapOf(
                        FIELD_STATUS_EVENT_STATUS to OrderStatus.PLACED.name,
                        FIELD_STATUS_EVENT_AT to now,
                    ),
                ),
            )
            put(FIELD_CREATED_AT, now)
            put(FIELD_UPDATED_AT, now)
            put(FIELD_SCAN_ID, draft.sizing.scanId)
            put(FIELD_SUIT_SIZE, draft.sizing.suitSize)
            put(FIELD_HEIGHT_CM, draft.sizing.heightCm)
            put(FIELD_WEIGHT_KG, draft.sizing.weightKg)
            put(FIELD_CONTACT, draft.contact.toMap())
            put(FIELD_SHIPPING_ADDRESS, draft.shippingAddress.toMap())
        }.filterValues { it != null }

        docRef.set(data).await()
        Timber.d("placeOrder: wrote %s (%s)", docRef.id, orderNumber)

        Order(
            id = docRef.id,
            orderNumber = orderNumber,
            status = OrderStatus.PLACED,
            statusHistory = listOf(OrderStatusEvent(OrderStatus.PLACED, now.toDate().time)),
            createdAtMillis = now.toDate().time,
            etaMillis = null,
            trackingNumber = null,
            carrier = null,
            suitId = null,
            sizing = draft.sizing,
            contact = draft.contact,
            shippingAddress = draft.shippingAddress,
        )
    }

    override suspend fun getLatestActiveOrder(): Result<Order?> = runCatching {
        ordersCollection()
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(LATEST_ORDER_LOOKBACK)
            .get()
            .await()
            .documents
            .mapNotNull { it.toOrder() }
            .firstOrNull { it.status != OrderStatus.CANCELLED }
    }

    override suspend fun updateShippingAddress(
        orderId: String,
        address: OrderShippingAddress,
    ): Result<Unit> = runCatching {
        ordersCollection().document(orderId)
            .update(
                mapOf(
                    FIELD_SHIPPING_ADDRESS to address.toMap(),
                    FIELD_UPDATED_AT to Timestamp.now(),
                ),
            )
            .await()
    }

    override suspend fun activateSuit(orderId: String, suitId: String): Result<Unit> = runCatching {
        ordersCollection().document(orderId)
            .update(
                mapOf(
                    FIELD_SUIT_ID to suitId,
                    FIELD_UPDATED_AT to Timestamp.now(),
                ),
            )
            .await()
    }

    private fun OrderContact.toMap(): Map<String, String> = mapOf(
        FIELD_FIRST_NAME to firstName,
        FIELD_LAST_NAME to lastName,
        FIELD_EMAIL to email,
        FIELD_PHONE_COUNTRY_ISO to phoneCountryIso,
        FIELD_PHONE_DIAL_CODE to phoneDialCode,
        FIELD_PHONE_NUMBER to phoneNumber,
    )

    private fun OrderShippingAddress.toMap(): Map<String, String> = mapOf(
        FIELD_COUNTRY_ISO to countryIso,
        FIELD_COUNTRY_NAME to countryName,
        FIELD_COMPANY to company,
        FIELD_ADDRESS_LINE to addressLine,
        FIELD_APARTMENT to apartment,
        FIELD_CITY to city,
        FIELD_REGION to region,
        FIELD_POSTAL_CODE to postalCode,
        FIELD_NOTE to note,
    )

    private fun DocumentSnapshot.toOrder(): Order? {
        val status = enumOrNull(getString(FIELD_STATUS)) ?: return null
        return Order(
            id = id,
            orderNumber = getString(FIELD_ORDER_NUMBER) ?: return null,
            status = status,
            statusHistory = parseStatusHistory(),
            createdAtMillis = getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
            etaMillis = getTimestamp(FIELD_ETA)?.toDate()?.time,
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
            shippingAddress = parseShippingAddress(),
        )
    }

    private fun DocumentSnapshot.parseStatusHistory(): List<OrderStatusEvent> {
        val raw = get(FIELD_STATUS_HISTORY) as? List<*> ?: return emptyList()
        return raw.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val status = enumOrNull(map[FIELD_STATUS_EVENT_STATUS] as? String) ?: return@mapNotNull null
            val at = (map[FIELD_STATUS_EVENT_AT] as? Timestamp)?.toDate()?.time ?: return@mapNotNull null
            OrderStatusEvent(status = status, atMillis = at)
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

    private fun DocumentSnapshot.parseShippingAddress(): OrderShippingAddress {
        val map = get(FIELD_SHIPPING_ADDRESS) as? Map<*, *> ?: emptyMap<Any, Any>()
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

    private fun enumOrNull(name: String?): OrderStatus? =
        OrderStatus.entries.firstOrNull { it.name == name }
}
