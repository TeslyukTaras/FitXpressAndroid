package com.hexis.bi.ui.main.buysuit.orderdetails

/**
 * One step of the order status ladder. [timestamp] is the time the step was reached, or — for a
 * still-pending step — the admin's estimated time, if one is set; null when neither exists.
 */
data class OrderTimelineStepUi(
    val label: String,
    val timestamp: String?,
    val reached: Boolean,
)

/** Display-ready model for the Order Details bottom sheet; built by the hosting ViewModel. */
data class OrderDetailsUi(
    /** Order document id, for the edit-address flow. */
    val orderId: String,
    /** Header value: the tracking number once shipped, otherwise the order number. */
    val reference: String,
    /** True when [reference] is a tracking number (drives the header label + copy description). */
    val referenceIsTracking: Boolean,
    /** Short date like "Dec 29"; null until an ETA is known. */
    val eta: String?,
    val steps: List<OrderTimelineStepUi>,
    /** Single-line shipping address (the pending requested one if a change is in flight). */
    val address: String,
    /** Address can still be changed (order not shipped/delivered/cancelled). */
    val canEditAddress: Boolean,
    /** A submitted address change is awaiting the admin. */
    val addressChangePending: Boolean,
)
