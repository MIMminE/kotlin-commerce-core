package nuts.commerce.orderservice.adapter.inbound

import nuts.commerce.orderservice.event.InboundEventType.*
import nuts.commerce.orderservice.event.OrderInboundEvent
import nuts.commerce.orderservice.handle.PaymentConfirmHandler
import nuts.commerce.orderservice.handle.PaymentCreateFailHandler
import nuts.commerce.orderservice.handle.PaymentCreateSuccessHandler
import nuts.commerce.orderservice.handle.PaymentReleaseHandler
import nuts.commerce.orderservice.handle.ReservationCreateSuccessHandler
import nuts.commerce.orderservice.handle.ReservationConfirmEventHandler
import nuts.commerce.orderservice.handle.ReservationCreateFailHandler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "order.kafka.inbound.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val reservationCreateSuccessHandler: ReservationCreateSuccessHandler,
    private val reservationCreateFailHandler: ReservationCreateFailHandler,
    private val reservationConfirmEventHandler: ReservationConfirmEventHandler,
    private val paymentCreateSuccessHandler: PaymentCreateSuccessHandler,
    private val paymentCreateFailHandler: PaymentCreateFailHandler,
    private val paymentConfirmHandler: PaymentConfirmHandler,
    private val paymentReleaseHandler: PaymentReleaseHandler
) {
    @KafkaListener(
        topics = [$$"${order.kafka.inbound.topic}"],
        groupId = $$"${order.kafka.inbound.group-id}",
    )
    fun onMessage(
        @Payload inboundEvent: OrderInboundEvent
    ) {
        when (inboundEvent.eventType) {
            RESERVATION_CREATION_SUCCEEDED -> reservationCreateSuccessHandler.execute(inboundEvent)
            RESERVATION_CREATION_FAILED -> reservationCreateFailHandler.handle(inboundEvent)
            RESERVATION_CONFIRM -> reservationConfirmEventHandler.handle(inboundEvent)
            PAYMENT_CREATION_SUCCEEDED -> paymentCreateSuccessHandler.handle(inboundEvent)
            PAYMENT_CREATION_FAILED -> paymentCreateFailHandler.handle(inboundEvent)
            PAYMENT_CONFIRM -> paymentConfirmHandler.handle(inboundEvent)
            PAYMENT_RELEASE -> paymentReleaseHandler.handle(inboundEvent)
        }
    }
}