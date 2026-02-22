package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.orderservice.event.outbound.ReservationConfirmPayloadReservation
import nuts.commerce.orderservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmRequestEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<ReservationOutboundEvent> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_CONFIRM_REQUEST

    override fun convert(outboxInfo: OutboxInfo): ReservationOutboundEvent {
        return ReservationOutboundEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, ReservationConfirmPayloadReservation::class.java)
        )
    }
}

