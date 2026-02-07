package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.OrderRepository
import nuts.commerce.orderservice.application.port.repository.OrderSagaRepository
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderSaga
import nuts.commerce.orderservice.model.exception.OrderException
import nuts.commerce.orderservice.model.infra.OutboxEventType
import nuts.commerce.orderservice.model.infra.OutboxRecord
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

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

        // 주문 상태 전이: PAYING -> PAYMENT_FAILED
        try {
            order.applyPaymentFailed()
        } catch (e: OrderException.InvalidTransition) {
            // 전이 불가능한 경우 upstream에서 잘못된 메시지 순서일 수 있으므로 예외 전파
            throw e
        }
        orderRepository.save(order)

        // 사가 조회 및 보상(재고 반납) 처리
        val saga = orderSagaRepository.findByOrderId(order.id)
        var inventoryWasReserved = false
        if (saga != null) {
            // 이미 실패 처리된 경우 중복 처리 방지
            if (saga.status == OrderSaga.SagaStatus.FAILED) return

            try {
                // 사가가 이미 INVENTORY_RESERVED 또는 그 이전 단계였다면 보상 대상
                if (saga.status == OrderSaga.SagaStatus.INVENTORY_RESERVED ||
                    saga.status == OrderSaga.SagaStatus.INVENTORY_REQUESTED ||
                    saga.status == OrderSaga.SagaStatus.PAYMENT_REQUESTED
                ) {
                    inventoryWasReserved = true
                }

                // 가능한 경우 재고 반환으로 전이
                try {
                    saga.markInventoryReleased()
                } catch (_: OrderException.InvalidTransition) {
                    // 만약 현재 사가 상태에서 inventory released로 직접 전이 불가하면 무시
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
                eventType = OutboxEventType.RESERVE_INVENTORY_RELEASE,
                payload = payloadJson
            )

            orderOutboxRepository.save(outbox)
        }
    }

    data class PaymentFailedEvent(
        val eventId: String,
        val orderId: java.util.UUID,
        val reason: String,
    )
}