package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationReleaseSuccessPayload
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReservationReleaseHandler(
    private val sageRepository: SageRepository,
) : InboundEventHandler {

    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_RELEASE

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as ReservationReleaseSuccessPayload

        sageRepository.markReservationReleaseAt(orderId, payload.reason)
    }
}