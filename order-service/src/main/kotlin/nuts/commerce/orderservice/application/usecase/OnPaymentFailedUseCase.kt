package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.repository.OrderRepository
import org.springframework.stereotype.Service

@Service
class OnPaymentFailedUseCase(
    private val orderRepository: OrderRepository,
) {

    @Transactional
    fun handle(event: PaymentFailedEvent) {
        val order = orderRepository.findById(event.orderId) ?: return

        if (order.status.name == "PAYMENT_FAILED") return
        if (order.status.name == "PAID") return

        order.applyPaymentFailed()
        orderRepository.save(order)
    }

    data class PaymentFailedEvent(
        val eventId: String,
        val orderId: java.util.UUID,
        val reason: String,
    )
}