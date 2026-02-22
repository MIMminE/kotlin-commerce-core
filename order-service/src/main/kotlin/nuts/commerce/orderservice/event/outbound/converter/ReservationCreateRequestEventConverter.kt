package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OrderOutboundEvent
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayload
import nuts.commerce.orderservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreateRequestEventConverter(
    private val objectMapper: ObjectMapper
) : OutboundEventConverter {
    override val supportType: OutboundEventType
        get() = OutboundEventType.RESERVATION_CREATE_REQUEST

    override fun convert(outboxInfo: OutboxInfo): OrderOutboundEvent {
        return OrderOutboundEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, ReservationCreatePayload::class.java)
        )
    }
}