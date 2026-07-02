package com.hexis.bi.data.order

import com.hexis.bi.domain.order.OrderSizing

/**
 * In-memory holder for passing the confirmed size selection from "Your size results"
 * to "Shipping details" without serializing through navigation arguments
 * (same pattern as ScanResultRepository).
 */
class OrderDraftHolder {
    var sizing: OrderSizing? = null
    fun clear() {
        sizing = null
    }
}
