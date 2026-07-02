package com.hexis.bi.domain.order

/**
 * Lifecycle of a suit order. [Order.statusHistory] holds one [OrderStatusEvent] per ladder step,
 * all written at order creation; the Order Details timeline renders that list directly.
 */
enum class OrderStatus {
    PLACED,
    CONFIRMED,
    IN_PRODUCTION,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}

/**
 * One step of the order ladder. [estimatedAtMillis] is the (admin-set) time the step is expected
 * to be reached; [atMillis] is when it actually was — null while the step is still pending.
 */
data class OrderStatusEvent(
    val status: OrderStatus,
    val estimatedAtMillis: Long?,
    val atMillis: Long?,
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

enum class OrderAddressChangeStatus {
    /** Customer submitted a new address; the admin/fulfillment side is notified to apply it. */
    REQUESTED,

    /** Admin has applied the request to [Order.shippingAddress]. */
    APPLIED,
}

/**
 * A customer-submitted address change recorded on the order so the fulfillment side is notified.
 * It does not overwrite [Order.shippingAddress] — it's a notification/audit entry, not an approval
 * gate. The admin applies it (copies [address] into the order and flips [status] to APPLIED).
 */
data class OrderAddressChangeRequest(
    val address: OrderShippingAddress,
    val requestedAtMillis: Long,
    val status: OrderAddressChangeStatus,
)

data class Order(
    val id: String,
    val orderNumber: String,
    val status: OrderStatus,
    val statusHistory: List<OrderStatusEvent>,
    val createdAtMillis: Long,
    val trackingNumber: String?,
    val carrier: String?,
    /** Physical suit linked after delivery via the activation flow; null until then. */
    val suitId: String?,
    val sizing: OrderSizing,
    val contact: OrderContact,
    val shippingAddress: OrderShippingAddress,
    val addressChangeRequest: OrderAddressChangeRequest? = null,
) {
    /** Overall ETA = the estimate on the final ladder step (DELIVERED); null until set. */
    val estimatedDeliveryMillis: Long?
        get() = statusHistory.lastOrNull()?.estimatedAtMillis

    /** Address can be edited until the order ships (or is cancelled). */
    val isAddressEditable: Boolean
        get() = status != OrderStatus.SHIPPED &&
                status != OrderStatus.DELIVERED &&
                status != OrderStatus.CANCELLED

    /** A submitted address change is still awaiting the admin. */
    val hasPendingAddressChange: Boolean
        get() = addressChangeRequest?.status == OrderAddressChangeStatus.REQUESTED

    /** Address to display: the pending requested one if any, else the confirmed shipping address. */
    val effectiveShippingAddress: OrderShippingAddress
        get() = addressChangeRequest
            ?.takeIf { it.status == OrderAddressChangeStatus.REQUESTED }
            ?.address
            ?: shippingAddress
}
