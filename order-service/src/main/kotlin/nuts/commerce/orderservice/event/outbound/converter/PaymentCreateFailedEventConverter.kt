package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentCreateFailedPayload
import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.orderservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentCreateFailedEventConverter(private val objectMapper: ObjectMapper) :
    OutboundEventConverter<PaymentOutboundEvent> {
    override val supportType: OutboundEventType
        get() = OutboundEventType.PAYMENT_CREATE_FAILED

    override fun convert(outboxInfo: OutboxInfo): PaymentOutboundEvent {
        return PaymentOutboundEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, PaymentCreateFailedPayload::class.java)
        )
    }
}

