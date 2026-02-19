package nuts.commerce.productservice.adapter.inbound

import jakarta.annotation.PostConstruct
import nuts.commerce.productservice.adapter.inbound.ListenEventType.*
import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent
import nuts.commerce.productservice.usecase.ReservationCreationSucceededHandler
import nuts.commerce.productservice.usecase.ReservationReleaseHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "product.kafka.inbound.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val reservationCreationSucceededHandler: ReservationCreationSucceededHandler,
    private val reservationReleaseHandler: ReservationReleaseHandler
) {

    @KafkaListener(
        topics = [$$"${product.kafka.inbound.topic}"],
        groupId = $$"${product.kafka.inbound.group-id}"
    )
    fun onMessage(
        @Payload inboundEvent: ProductInboundEvent
    ) {
        when (inboundEvent.eventType) {
            InboundEventType.RESERVATION_CREATION_SUCCEEDED -> reservationCreationSucceededHandler.handle(inboundEvent)
            InboundEventType.RESERVATION_RELEASE -> reservationReleaseHandler.handle(inboundEvent)
        }
    }
}