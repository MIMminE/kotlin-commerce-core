package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayloadReservation
import nuts.commerce.orderservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationReleaseRequestEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ReservationOutboundEvent> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_RELEASE_REQUEST

    override fun convert(outboxInfo: OutboxInfo): ReservationOutboundEvent {
        return ReservationOutboundEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, ReservationReleasePayloadReservation::class.java)
        )
    }
}

