package com.hexis.bi.data.order

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hexis.bi.domain.order.Order
import com.hexis.bi.domain.order.OrderAddressChangeStatus
import com.hexis.bi.domain.order.OrderDraft
import com.hexis.bi.domain.order.OrderRepository
import com.hexis.bi.domain.order.OrderShippingAddress
import com.hexis.bi.domain.order.OrderStatus
import com.hexis.bi.domain.order.OrderStatusEvent
import com.hexis.bi.utils.constants.OrderFirestoreConstants.COLLECTION_ORDERS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_ADDRESS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_REQUEST
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_REQUESTED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ADDRESS_CHANGE_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CONTACT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_CREATED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_HEIGHT_CM
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_ORDER_NUMBER
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SCAN_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SHIPPING_ADDRESS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_EVENT_STATUS
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_STATUS_HISTORY
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_SUIT_SIZE
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_UPDATED_AT
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_USER_ID
import com.hexis.bi.utils.constants.OrderFirestoreConstants.FIELD_WEIGHT_KG
import com.hexis.bi.utils.constants.OrderFirestoreConstants.LATEST_ORDER_LOOKBACK
import com.hexis.bi.utils.constants.OrderFirestoreConstants.ORDER_NUMBER_ID_LENGTH
import com.hexis.bi.utils.constants.OrderFirestoreConstants.ORDER_NUMBER_PREFIX
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale

class FirestoreOrderRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : OrderRepository {

    private fun ordersCollection() = firestore.collection(COLLECTION_ORDERS)

    private fun currentUid(): String =
        auth.currentUser?.uid ?: error("Not authenticated")

    override suspend fun placeOrder(draft: OrderDraft): Result<Order> = runCatching {
        val uid = currentUid()
        val docRef = ordersCollection().document()
        val now = Timestamp.now()
        val orderNumber = ORDER_NUMBER_PREFIX +
                docRef.id.take(ORDER_NUMBER_ID_LENGTH).uppercase(Locale.US)

        // The whole ladder is written at creation: one record per step. `at` is set only for the
        // initial PLACED step; `estimated` is filled in later by the admin/fulfillment side.
        val statusHistory = LADDER_STATUSES.map { step ->
            buildMap<String, Any> {
                put(FIELD_STATUS_EVENT_STATUS, step.name)
                if (step == OrderStatus.PLACED) put(FIELD_STATUS_EVENT_AT, now)
            }
        }

        val data = buildMap<String, Any?> {
            put(FIELD_USER_ID, uid)
            put(FIELD_ORDER_NUMBER, orderNumber)
            put(FIELD_STATUS, OrderStatus.PLACED.name)
            put(FIELD_STATUS_HISTORY, statusHistory)
            put(FIELD_CREATED_AT, now)
            put(FIELD_UPDATED_AT, now)
            put(FIELD_SCAN_ID, draft.sizing.scanId)
            put(FIELD_SUIT_SIZE, draft.sizing.suitSize)
            put(FIELD_HEIGHT_CM, draft.sizing.heightCm)
            put(FIELD_WEIGHT_KG, draft.sizing.weightKg)
            put(FIELD_CONTACT, OrderFirestoreMapper.toMap(draft.contact))
            put(FIELD_SHIPPING_ADDRESS, OrderFirestoreMapper.toMap(draft.shippingAddress))
        }.filterValues { it != null }

        docRef.set(data).await()
        Timber.d("placeOrder: wrote %s (%s)", docRef.id, orderNumber)

        Order(
            id = docRef.id,
            orderNumber = orderNumber,
            status = OrderStatus.PLACED,
            statusHistory = LADDER_STATUSES.map { step ->
                OrderStatusEvent(
                    status = step,
                    estimatedAtMillis = null,
                    atMillis = if (step == OrderStatus.PLACED) now.toDate().time else null,
                )
            },
            createdAtMillis = now.toDate().time,
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
            .whereEqualTo(FIELD_USER_ID, currentUid())
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .limit(LATEST_ORDER_LOOKBACK)
            .get()
            .await()
            .documents
            .mapNotNull { OrderFirestoreMapper.toOrder(it) }
            .firstOrNull { it.status != OrderStatus.CANCELLED }
    }.onFailure {
        Timber.e(it, "getLatestActiveOrder failed")
    }

    override suspend fun getOrderById(orderId: String): Result<Order?> = runCatching {
        OrderFirestoreMapper.toOrder(ordersCollection().document(orderId).get().await())
    }.onFailure {
        Timber.e(it, "getOrderById failed")
    }

    override suspend fun updateShippingAddress(
        orderId: String,
        address: OrderShippingAddress,
    ): Result<Unit> = runCatching {
        ordersCollection().document(orderId)
            .update(
                mapOf(
                    FIELD_SHIPPING_ADDRESS to OrderFirestoreMapper.toMap(address),
                    FIELD_UPDATED_AT to Timestamp.now(),
                ),
            )
            .await()
    }

    override suspend fun requestAddressChange(
        orderId: String,
        address: OrderShippingAddress,
    ): Result<Unit> = runCatching {
        ordersCollection().document(orderId)
            .update(
                mapOf(
                    FIELD_ADDRESS_CHANGE_REQUEST to mapOf(
                        FIELD_ADDRESS_CHANGE_ADDRESS to OrderFirestoreMapper.toMap(address),
                        FIELD_ADDRESS_CHANGE_REQUESTED_AT to Timestamp.now(),
                        FIELD_ADDRESS_CHANGE_STATUS to OrderAddressChangeStatus.REQUESTED.name,
                    ),
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

    private companion object {
        /** The visible ladder, in order; CANCELLED is terminal/off-ladder and never a timeline step. */
        val LADDER_STATUSES = OrderStatus.entries.filter { it != OrderStatus.CANCELLED }
    }
}
