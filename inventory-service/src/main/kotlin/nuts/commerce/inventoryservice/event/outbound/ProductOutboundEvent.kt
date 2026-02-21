package nuts.commerce.inventoryservice.event.outbound

import java.util.UUID

data class ProductOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: ProductEventType,
    val payload: ProductEventPayload
)

sealed interface ProductEventPayload

data class ProductCreatedPayload(
    val productId: UUID,
    val stock: Long
) : ProductEventPayload

data class ProductStockIncrementPayload(
    val orderId: UUID,
    val productId: UUID,
    val qty: Long
) : ProductEventPayload

data class ProductStockDecrementPayload(
    val orderId: UUID,
    val productId: UUID,
    val qty: Long
) : ProductEventPayload

enum class ProductEventType {
    INCREMENT_STOCK,
    DECREMENT_STOCK,
    CREATED,
}