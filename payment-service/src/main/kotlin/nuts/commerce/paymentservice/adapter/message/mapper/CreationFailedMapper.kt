package nuts.commerce.paymentservice.adapter.message.mapper

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.port.message.PaymentCreationFailedEvent
import nuts.commerce.paymentservice.port.message.PaymentEvent
import nuts.commerce.paymentservice.port.message.PaymentEventMapper
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class CreationFailedMapper(private val objectMapper: ObjectMapper) : PaymentEventMapper {

    override val eventType: EventType
        get() = EventType.PAYMENT_CREATION_FAILED

    override fun map(outboxInfo: OutboxInfo): PaymentEvent {
        return PaymentCreationFailedEvent(
            outboxId = outboxInfo.outboxId,
            orderId = outboxInfo.orderId,
            paymentId = outboxInfo.paymentId,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                PaymentCreationFailedEvent.Payload::class.java
            )
        )
    }

}