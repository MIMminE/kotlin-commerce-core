package nuts.commerce.inventoryservice.event.outbound.converter

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationReleaseEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ReservationOutboundEvent, OutboundEventType> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_RELEASE

    override fun convert(outboxInfo: OutboxInfo): ReservationOutboundEvent {
        return ReservationOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationReleaseSuccessPayload::class.java
            )
        )
    }
}