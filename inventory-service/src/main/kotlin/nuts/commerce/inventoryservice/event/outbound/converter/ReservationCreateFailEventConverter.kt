package nuts.commerce.inventoryservice.event.outbound.converter

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreateFailEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ReservationOutboundEvent, OutboundEventType> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_CREATION_FAILED

    override fun convert(outboxInfo: OutboxInfo): ReservationOutboundEvent {

        return ReservationOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                ReservationCreationFailPayload::class.java
            )
        )
    }

}