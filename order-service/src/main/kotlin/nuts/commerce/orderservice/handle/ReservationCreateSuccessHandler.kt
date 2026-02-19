package nuts.commerce.orderservice.handle

import nuts.commerce.orderservice.event.*
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreateSuccessHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun execute(event: OrderInboundEvent) {

        require(event.eventType == InboundEventType.RESERVATION_CREATION_SUCCEEDED)
        require(event.payload is ReservationCreationSucceededPayload)

        val saga = sageRepository.findByOrderId(event.orderId)
            ?: throw IllegalStateException("Sage record not found for orderId: ${event.orderId}")

        val totalPrice = saga.totalPrice
        val currency = saga.currency

        val outboxRecord = OutboxRecord.create(
            orderId = event.orderId,
            idempotencyKey = event.eventId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            payload = objectMapper.writeValueAsString(
                PaymentCreatePayload(
                    amount = totalPrice,
                    currency = currency
                )
            )
        )

        orderRepository.updateStatus(event.orderId, OrderStatus.CREATED, OrderStatus.PAYING)
        sageRepository.markPaymentRequestAt(event.orderId)
        outboxRepository.save(outboxRecord)
    }
}