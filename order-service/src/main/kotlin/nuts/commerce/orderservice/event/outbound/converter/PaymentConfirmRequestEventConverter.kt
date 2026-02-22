package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.orderservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentConfirmRequestEventConverter(
    private val objectMapper: ObjectMapper
) : OutboundEventConverter<PaymentOutboundEvent> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.PAYMENT_CONFIRM_REQUEST

    override fun convert(outboxInfo: OutboxInfo): PaymentOutboundEvent {
        return PaymentOutboundEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, PaymentConfirmPayload::class.java)
        )
    }
}

