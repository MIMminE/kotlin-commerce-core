package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationCreationSucceededPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
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
    private val objectMapper: ObjectMapper
) : OrderEventHandler {

    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_CREATION_SUCCEEDED

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as ReservationCreationSucceededPayload

        val saga = sageRepository.findByOrderId(orderId)
            ?: throw IllegalStateException("Sage record not found for orderId: $orderId")

        val totalPrice = saga.totalPrice
        val currency = saga.currency

        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            idempotencyKey = eventId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            payload = objectMapper.writeValueAsString(
                PaymentCreatePayload(
                    amount = totalPrice,
                    currency = currency
                )
            )
        )

        orderRepository.updateStatus(orderId, OrderStatus.CREATED, OrderStatus.PAYING)
        sageRepository.markPaymentRequestAt(orderId)
        outboxRepository.save(outboxRecord)
    }
}