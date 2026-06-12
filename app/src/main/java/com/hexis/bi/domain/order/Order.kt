package com.hexis.bi.domain.order

/**
 * Lifecycle of a suit order. [statusHistory] on [Order] records when each step
 * was reached; the Order Details timeline renders this ladder and lights the
 * steps present in the history.
 */
enum class OrderStatus {
    PLACED,
    CONFIRMED,
    IN_PRODUCTION,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}

data class OrderStatusEvent(
    val status: OrderStatus,
    val atMillis: Long,
)

data class OrderContact(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneCountryIso: String,
    val phoneDialCode: String,
    val phoneNumber: String,
)

data class OrderShippingAddress(
    val countryIso: String,
    val countryName: String,
    val company: String,
    val addressLine: String,
    val apartment: String,
    val city: String,
    val region: String,
    val postalCode: String,
    val note: String,
)

/**
 * Size recommendation confirmed on the "Your size results" screen, snapshotted
 * into the order so a later rescan doesn't rewrite what was ordered.
 */
data class OrderSizing(
    val scanId: String?,
    val suitSize: String,
    val heightCm: Float,
    val weightKg: Float,
)

/** Everything needed to place an order; the repository fills in id, number, status and timestamps. */
data class OrderDraft(
    val contact: OrderContact,
    val shippingAddress: OrderShippingAddress,
    val sizing: OrderSizing,
)

data class Order(
    val id: String,
    val orderNumber: String,
    val status: OrderStatus,
    val statusHistory: List<OrderStatusEvent>,
    val createdAtMillis: Long,
    val etaMillis: Long?,
    val trackingNumber: String?,
    val carrier: String?,
    /** Physical suit linked after delivery via the activation flow; null until then. */
    val suitId: String?,
    val sizing: OrderSizing,
    val contact: OrderContact,
    val shippingAddress: OrderShippingAddress,
)
