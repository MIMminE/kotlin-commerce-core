package nuts.commerce.orderservice.port.message

import nuts.commerce.orderservice.event.EventType
import java.util.UUID

interface OrderEventProducer {
    fun produce(orderEvent: OrderEvent)
}

@ConsistentCopyVisibility
data class OrderEvent internal constructor(
    val eventId: UUID,
    val outboxId: UUID,
    val orderId: UUID,
    val eventType: EventType,
    val payload: String
) {
    companion object {
        fun create(
            orderId: UUID,
            outboxId: UUID,
            eventType: EventType,
            payload: String,
            eventId: UUID = UUID.randomUUID(),
        ): OrderEvent =
            OrderEvent(
                eventId = eventId,
                outboxId = outboxId,
                orderId = orderId,
                eventType = eventType,
                payload = payload
            )
    }
}