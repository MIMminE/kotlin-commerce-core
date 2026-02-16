package nuts.commerce.inventoryservice.adapter.message.mapper

import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.ReservationConfirmEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmEventMapper(private val objectMapper: ObjectMapper) : InventoryEventMapper {
    override val eventType: EventType
        get() = EventType.RESERVATION_CONFIRM

    override fun map(outboxInfo: OutboxInfo): InventoryEvent {
        return ReservationConfirmEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            reservationId = outboxInfo.reservationId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationConfirmEvent.Payload::class.java
            )
        )
    }
}