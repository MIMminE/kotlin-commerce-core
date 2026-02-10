package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.model.EventType
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface InventoryEventProducer {
    fun produce(inventoryEvent: InventoryEvent): CompletableFuture<ProduceResult>
}

@ConsistentCopyVisibility
data class InventoryEvent internal constructor(
    val eventId: UUID,
    val outboxId: UUID,
    val orderId: UUID,
    val reservationId: UUID,
    val eventType: EventType,
    val payload: String

) {
    companion object {
        fun create(
            orderId: UUID,
            outboxId: UUID,
            reservationId: UUID,
            eventType: EventType,
            payload: String,
            eventId: UUID = UUID.randomUUID(),
        ): InventoryEvent =
            InventoryEvent(
                eventId = eventId,
                outboxId = outboxId,
                orderId = orderId,
                reservationId = reservationId,
                eventType = eventType,
                payload = payload
            )
    }
}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}