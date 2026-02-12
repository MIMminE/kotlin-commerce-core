package nuts.commerce.paymentservice.adapter.message.mapper

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.port.message.PaymentEvent
import nuts.commerce.paymentservice.port.message.PaymentEventMapper
import nuts.commerce.paymentservice.port.message.PaymentReleaseFailedEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReleaseFailedMapper(private val objectMapper: ObjectMapper) : PaymentEventMapper {
    override val eventType: EventType
        get() = EventType.PAYMENT_RELEASE_FAILED

    override fun map(outboxInfo: OutboxInfo): PaymentEvent {
        return PaymentReleaseFailedEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            paymentId = outboxInfo.paymentId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                PaymentReleaseFailedEvent.Payload::class.java
            )
        )
    }
}