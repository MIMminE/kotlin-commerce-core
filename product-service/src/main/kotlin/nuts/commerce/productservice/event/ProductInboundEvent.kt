package nuts.commerce.productservice.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class ProductInboundEvent(
    val eventId: UUID,
    val eventType: InboundEventType,

    @field:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "eventType"
    )
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = ProductCreatedPayload::class, name = "CREATED"),
        JsonSubTypes.Type(value = ProductStockIncrementPayload::class, name = "INCREMENT_STOCK"),
        JsonSubTypes.Type(value = ProductStockDecrementPayload::class, name = "DECREMENT_STOCK")
    )
    val payload: InboundPayload
)

sealed interface InboundPayload

data class ProductCreatedPayload(
    val productId: UUID,
    val stock: Long
) : InboundPayload

data class ProductStockIncrementPayload(
    val orderId: UUID,
    val productId: UUID,
    val qty: Long
) : InboundPayload

data class ProductStockDecrementPayload(
    val orderId: UUID,
    val productId: UUID,
    val qty: Long
) : InboundPayload

enum class InboundEventType {
    INCREMENT_STOCK,
    DECREMENT_STOCK,
    CREATED
}