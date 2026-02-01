package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.repository.OrderRepository
import org.springframework.stereotype.Service

@Service
class OnPaymentApprovedUseCase(
    private val orderRepository: OrderRepository,
    // private val processedEventRepository: ProcessedEventRepository (권장: 멱등)
) {

    @Transactional
    fun handle(event: PaymentApprovedEvent) {
        // (권장) 멱등: event.eventId를 유니크로 저장하고 이미 처리했으면 return
        // if (processedEventRepository.isProcessed(event.eventId)) return

        val order = orderRepository.findById(event.orderId)
            ?: return // 이벤트가 늦게 와서 주문이 없으면 무시/DLQ는 정책에 따라

        // 이미 PAID면 중복 이벤트로 보고 no-op 처리(멱등)
        if (order.status.name == "PAID") return

        order.applyPaymentApproved()
        orderRepository.save(order)

        // processedEventRepository.markProcessed(event.eventId)
    }

    data class PaymentApprovedEvent(
        val eventId: String,
        val orderId: java.util.UUID,
        val paymentId: String,
    )
}