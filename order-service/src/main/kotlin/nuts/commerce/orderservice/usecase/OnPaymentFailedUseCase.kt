package nuts.commerce.orderservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.event.EventType
import nuts.commerce.orderservice.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.exception.OrderException
import nuts.commerce.orderservice.model.OutboxRecord
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class OnPaymentFailedUseCase(
    private val orderRepository: OrderRepository,
    private val orderSagaRepository: OrderSagaRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun handle(event: PaymentFailedEvent) {
        val order = orderRepository.findById(event.orderId) ?: return

        // idempotency: 이미 실패 처리되어 있거나 이미 결제된 주문이면 아무 작업도 하지 않음
        if (order.status == Order.OrderStatus.PAYMENT_FAILED) return
        if (order.status == Order.OrderStatus.PAID) return

        try {
            order.applyPaymentFailed()
        } catch (e: OrderException.InvalidTransition) {
            throw e
        }
        orderRepository.save(order)

        val saga = orderSagaRepository.findByOrderId(order.id)
        var inventoryWasReserved = false
        if (saga != null) {
            if (saga.status == OrderSaga.SagaStatus.FAILED) return

            try {
                if (saga.status == OrderSaga.SagaStatus.INVENTORY_RESERVED ||
                    saga.status == OrderSaga.SagaStatus.INVENTORY_REQUESTED ||
                    saga.status == OrderSaga.SagaStatus.PAYMENT_REQUESTED
                ) {
                    inventoryWasReserved = true
                }

                try {
                    saga.markInventoryReleased()
                } catch (_: OrderException.InvalidTransition) {
                }

                // 최종적으로 사가를 실패로 표시
                saga.fail()
                orderSagaRepository.save(saga)

            } catch (e: OrderException.InvalidTransition) {
                // 사가 전이 실패 시 재시도 로직이나 알림 필요 — 여기서는 예외를 던집니다
                throw e
            }
        }

        // 보상 작업: 재고 반환을 위한 아웃박스 이벤트 생성
        // 아웃박스은 실제로 재고가 예약되었을 가능성이 있는 경우에만 생성
        if (inventoryWasReserved) {
            val itemsPayload = order.items.map { item ->
                mapOf("productId" to item.productId, "qty" to item.qty)
            }
            val payloadMap = mapOf("orderId" to order.id.toString(), "items" to itemsPayload, "reason" to event.reason)
            val payloadJson = objectMapper.writeValueAsString(payloadMap)

            val outbox = OutboxRecord.create(
                aggregateId = order.id,
                eventType = EventType.RESERVE_INVENTORY_RELEASE,
                payload = payloadJson
            )

            orderOutboxRepository.save(outbox)
        }
    }

    data class PaymentFailedEvent(
        val eventId: String,
        val orderId: UUID,
        val reason: String,
    )
}