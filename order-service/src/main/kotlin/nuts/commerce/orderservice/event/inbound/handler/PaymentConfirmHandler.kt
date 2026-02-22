package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.PaymentConfirmSuccessPayload
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentConfirmHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository
) : OrderEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.PAYMENT_CONFIRM

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as PaymentConfirmSuccessPayload

        orderRepository.updateStatus(orderId, OrderStatus.PAID, OrderStatus.COMPLETED)
        sageRepository.markPaymentCompleteAt(orderId)
        sageRepository.markCompleteAt(orderId)
    }
}