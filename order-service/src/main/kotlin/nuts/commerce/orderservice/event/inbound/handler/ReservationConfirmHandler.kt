package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.ReservationConfirmSuccessPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : InboundEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_CONFIRM

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as ReservationConfirmSuccessPayload

        val order = orderRepository.findById(orderId)
            ?: throw IllegalArgumentException("Invalid order ID: $orderId")

        val paymentId = sageRepository.findSageInfoByOrderId(orderId)?.let {
            it.paymentId ?: throw IllegalStateException("Payment ID not found in sage info for order $orderId")
        } ?: throw IllegalStateException("Sage info not found for order $orderId")


        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            idempotencyKey = eventId,
            eventType = OutboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = objectMapper.writeValueAsString(
                PaymentConfirmPayload(paymentId = paymentId)
            )
        )

        sageRepository.markReservationCompleteAt(orderId)
        outboxRepository.save(outboxRecord)
    }
}