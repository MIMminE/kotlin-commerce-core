package nuts.commerce.inventoryservice.adapter.message.mapper

import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.ReservationCreationFailedEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreationFailedEventMapper(private val objectMapper: ObjectMapper) : InventoryEventMapper {
    override val eventType: EventType
        get() = EventType.RESERVATION_CREATION_FAILED

    override fun map(outboxInfo: OutboxInfo): InventoryEvent {
        return ReservationCreationFailedEvent(
            outboxId = outboxInfo.outboxId,
            reservationId = outboxInfo.reservationId,
            orderId = outboxInfo.orderId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationCreationFailedEvent.Payload::class.java
            )
        )
    }
}