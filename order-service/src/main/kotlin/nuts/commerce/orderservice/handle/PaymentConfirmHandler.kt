package nuts.commerce.orderservice.handle

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
    private val sageRepository: SageRepository,
) {
    @Transactional
    fun handle(event: OrderInboundEvent) {
        require(event.eventType == InboundEventType.PAYMENT_CONFIRM)
        val payload = event.payload as PaymentConfirmSuccessPayload

        orderRepository.updateStatus(event.orderId, OrderStatus.PAID, OrderStatus.COMPLETED)
        sageRepository.markPaymentCompleteAt(event.orderId)
        sageRepository.markCompleteAt(event.orderId)
    }
}