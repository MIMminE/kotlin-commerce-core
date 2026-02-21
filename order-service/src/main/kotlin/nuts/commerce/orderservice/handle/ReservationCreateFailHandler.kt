package nuts.commerce.orderservice.handle

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
) {
    @Transactional
    fun handle(event: OrderInboundEvent) {

        require(event.eventType == InboundEventType.RESERVATION_CREATION_FAILED)
        require(event.payload is ReservationCreationFailedPayload)

        val payload = event.payload

        orderRepository.updateStatus(event.orderId, OrderStatus.CREATED, OrderStatus.FAIL)
        sageRepository.markFailedAt(event.orderId, payload.reason)
    }
}