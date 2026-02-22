package nuts.commerce.paymentservice.event.outbound.converter

import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.model.OutboxInfo
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PaymentCreateSuccessEventConverter(private val objectMapper: ObjectMapper) : OutboundEventConverter {

    override val supportType: OutboundEventType
        get() = OutboundEventType.PAYMENT_CREATION_SUCCEEDED

    override fun convert(outboxInfo: OutboxInfo): PaymentOutboundEvent {
        return PaymentOutboundEvent(
            orderId = outboxInfo.orderId,
            paymentId = outboxInfo.paymentId,
            outboxId = outboxInfo.outboxId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload, PaymentCreationSuccessPayload::class.java
            )
        )
    }
}