package nuts.commerce.paymentservice.adapter.message.mapper

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.port.message.PaymentEvent
import nuts.commerce.paymentservice.port.message.PaymentEventMapper
import nuts.commerce.paymentservice.port.message.PaymentReleasedEvent
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ReleaseSucceededMapper(private val objectMapper: ObjectMapper) : PaymentEventMapper {
    override val eventType: EventType
        get() = EventType.PAYMENT_RELEASE_SUCCEEDED

    override fun map(outboxInfo: OutboxInfo): PaymentEvent {
        return PaymentReleasedEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            paymentId = outboxInfo.paymentId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                PaymentReleasedEvent.Payload::class.java
            )
        )
    }

}