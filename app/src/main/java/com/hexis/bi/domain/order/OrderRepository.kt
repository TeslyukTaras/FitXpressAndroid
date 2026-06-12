package com.hexis.bi.domain.order

interface OrderRepository {
    suspend fun placeOrder(draft: OrderDraft): Result<Order>

    /** Most recent non-cancelled order — drives the Home "Suit order" card and Order Details sheet. */
    suspend fun getLatestActiveOrder(): Result<Order?>

    /** Address stays editable until the order ships; the UI gates on [Order.status]. */
    suspend fun updateShippingAddress(orderId: String, address: OrderShippingAddress): Result<Unit>

    /**
     * Links the delivered physical suit to the order. Once the shared suit-ID
     * registry exists, [suitId] should be validated against it before linking.
     */
    suspend fun activateSuit(orderId: String, suitId: String): Result<Unit>
}
