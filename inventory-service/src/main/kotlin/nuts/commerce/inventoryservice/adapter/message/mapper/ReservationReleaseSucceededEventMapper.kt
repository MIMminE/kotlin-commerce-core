package nuts.commerce.inventoryservice.adapter.message.mapper

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.InventoryEventMapper
import nuts.commerce.inventoryservice.port.message.ReservationReleaseSucceededEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationReleaseSucceededEventMapper(private val objectMapper: ObjectMapper) : InventoryEventMapper {
    override val eventType: EventType
        get() = EventType.RESERVATION_RELEASE_SUCCEEDED

    override fun map(outboxInfo: OutboxInfo): InventoryEvent {
        return ReservationReleaseSucceededEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            reservationId = outboxInfo.reservationId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationReleaseSucceededEvent.Payload::class.java
            )
        )
    }
}