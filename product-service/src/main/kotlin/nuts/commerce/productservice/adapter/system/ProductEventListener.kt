package nuts.commerce.productservice.adapter.system

import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent
import nuts.commerce.productservice.event.handler.ProductEventHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.product-event-listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class ProductEventListener(handlers: List<ProductEventHandler>) {
    private val handlerMap: Map<InboundEventType, ProductEventHandler> =
        handlers.associateBy { it.supportType }

    @KafkaListener(
        topics = [$$"${system.product-event-listener.topic}"],
        groupId = $$"${system.product-event-listener.group-id}",
    )
    fun onMessage(
        @Payload inboundEvent: ProductInboundEvent
    ) {
        handlerMap[inboundEvent.eventType]?.handle(inboundEvent)
            ?: throw IllegalArgumentException("No handler found for event type: ${inboundEvent.eventType}")
    }
}