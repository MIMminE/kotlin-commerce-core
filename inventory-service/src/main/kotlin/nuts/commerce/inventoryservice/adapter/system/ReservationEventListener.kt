package nuts.commerce.inventoryservice.adapter.system

import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.inbound.handler.ReservationEventHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.reservation-event-listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class ReservationEventListener(handlers: List<ReservationEventHandler>) {
    private val handlerMap = handlers.associateBy { it.supportType }

    @KafkaListener(
        topics = [$$"${system.reservation-event-listener.topic}"],
        groupId = $$"${system.reservation-event-listener.group-id}",
    )
    fun onMessage(@Payload inboundEvent: ReservationInboundEvent) {
        handlerMap[inboundEvent.eventType]?.handle(inboundEvent)
            ?: throw IllegalArgumentException("No handler found for event type: ${inboundEvent.eventType}")
    }
}