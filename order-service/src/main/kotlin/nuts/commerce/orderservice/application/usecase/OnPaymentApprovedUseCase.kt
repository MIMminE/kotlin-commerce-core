package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.OrderRepository
import nuts.commerce.orderservice.application.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.exception.OrderException
import nuts.commerce.orderservice.model.infra.OutboxEventType
import nuts.commerce.orderservice.model.infra.OutboxRecord
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class OnPaymentApprovedUseCase(
    private val orderRepository: OrderRepository,
    private val orderSagaRepository: OrderSagaRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun handle(event: PaymentApprovedEvent) {
        val order = orderRepository.findById(event.orderId) ?: return

        if (order.status == Order.OrderStatus.PAID) return

        try {
            order.applyPaymentApproved()
        } catch (e: OrderException.InvalidTransition) {
            throw e
        }

        orderRepository.save(order)

        val saga = orderSagaRepository.findByOrderId(order.id)
        if (saga != null) {
            saga.markPaymentCompleted()
            orderSagaRepository.save(saga)
        }

        // 아웃박스에 결제 완료 이벤트 저장
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "paymentId" to event.paymentId.toString(),
                "payload" to event.payload
            )
        )
        val outbox = OutboxRecord.create(
            aggregateId = order.id,
            eventType = OutboxEventType.PAYMENT_COMPLETED,
            payload = payload
        )

        orderOutboxRepository.save(outbox)
    }

    data class PaymentApprovedEvent(
        val eventId: UUID,
        val orderId: UUID,
        val paymentId: UUID,
        val payload: String,
    )
}