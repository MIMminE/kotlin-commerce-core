package nuts.commerce.inventoryservice.adapter.message.mapper

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.InventoryEventMapper
import nuts.commerce.inventoryservice.port.message.ReservationReleaseFailedEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationReleaseFailedEventMapper(private val objectMapper: ObjectMapper) : InventoryEventMapper {
    override val eventType: EventType
        get() = EventType.RESERVATION_RELEASE_FAILED

    override fun map(outboxInfo: OutboxInfo): InventoryEvent {
        return ReservationReleaseFailedEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            reservationId = outboxInfo.reservationId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationReleaseFailedEvent.Payload::class.java
            )
        )
    }
}