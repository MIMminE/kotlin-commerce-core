package nuts.commerce.paymentservice.adapter.system

import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.inbound.handler.PaymentEventHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.util.UUID

@ConditionalOnProperty(
    prefix = "payment-event-listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(handlers: List<PaymentEventHandler>) {
    private val handlerMap = handlers.associateBy { it.supportType }

    @KafkaListener(
        topics = [$$"${system.payment-event-listener.topic}"],
        groupId = $$"${system.payment-event-listener.group-id}",
    )
    fun onMessage(@Payload inboundEvent: PaymentInboundEvent) {
        handlerMap[inboundEvent.eventType]?.handle(inboundEvent)
            ?: throw IllegalArgumentException("No handler found for event type: ${inboundEvent.eventType}")
    }
}