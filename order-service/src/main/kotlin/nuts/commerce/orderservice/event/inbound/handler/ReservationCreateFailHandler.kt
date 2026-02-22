package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationCreationFailedPayload
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReservationCreateFailHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
) : OrderEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_CREATION_FAILED

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as ReservationCreationFailedPayload

        orderRepository.updateStatus(orderId, OrderStatus.CREATED, OrderStatus.FAIL)
        sageRepository.markFailedAt(orderId, payload.reason)
    }
}