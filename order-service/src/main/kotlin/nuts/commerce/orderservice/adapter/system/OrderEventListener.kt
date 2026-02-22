package nuts.commerce.orderservice.adapter.system

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.handler.OrderEventHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.order-event-listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class OrderEventListener(
    orderEventHandlerList: List<OrderEventHandler>
) {
    private val orderEventListenerMap: Map<InboundEventType, OrderEventHandler> =
        orderEventHandlerList.associateBy { it.supportType }

    @KafkaListener(
        topics = [$$"${system.order-event-listener.topic}"],
        groupId = $$"${system.order-event-listener.group-id}",
    )
    fun onMessage(
        @Payload inboundEvent: OrderInboundEvent
    ) {
        val handler = orderEventListenerMap[inboundEvent.eventType]
            ?: throw IllegalArgumentException("No handler found for event type: ${inboundEvent.eventType}")

        handler.handle(inboundEvent)
    }
}