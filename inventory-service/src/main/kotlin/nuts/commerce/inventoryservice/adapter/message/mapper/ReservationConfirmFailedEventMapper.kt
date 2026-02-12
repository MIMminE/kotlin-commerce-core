package nuts.commerce.inventoryservice.adapter.message.mapper

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.InventoryEventMapper
import nuts.commerce.inventoryservice.port.message.ReservationConfirmFailedEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmFailedEventMapper(private val objectMapper: ObjectMapper) : InventoryEventMapper {
    override val eventType: EventType
        get() = EventType.RESERVATION_CONFIRM_FAILED

    override fun map(outboxInfo: OutboxInfo): InventoryEvent {
        return ReservationConfirmFailedEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            reservationId = outboxInfo.reservationId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationConfirmFailedEvent.Payload::class.java
            )
        )
    }
}