package nuts.project.commerce.application.usecase.dto

import java.util.UUID

data class PlaceOrderCommand(
    val userId: UUID,
    val items: List<Item>,
    val couponId: UUID? = null,
    val commandIdempotencyKey: UUID
) {
    data class Item(val productId: UUID, val qty: Long)
}

data class PlaceOrderResult(
    val orderId: UUID,
    val originalAmount: Long,
    val discountAmount: Long,
    val finalAmount: Long
)