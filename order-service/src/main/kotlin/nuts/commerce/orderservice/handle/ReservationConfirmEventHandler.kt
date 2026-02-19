package nuts.commerce.orderservice.handle

import nuts.commerce.orderservice.event.InboundEventType
import nuts.commerce.orderservice.event.OrderInboundEvent
import nuts.commerce.orderservice.event.OutboundEventType
import nuts.commerce.orderservice.event.PaymentConfirmPayload
import nuts.commerce.orderservice.event.ReservationConfirmPayload
import nuts.commerce.orderservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmEventHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun handle(event: OrderInboundEvent) {

        require(event.eventType == InboundEventType.RESERVATION_CONFIRM)
        require(event.payload is ReservationConfirmSuccessPayload)

        val paymentId = sageRepository.findSageInfoByOrderId(event.orderId)?.let {
            it.paymentId ?: throw IllegalStateException("Payment ID not found in sage info for order ${event.orderId}")
        } ?: throw IllegalStateException("Sage info not found for order ${event.orderId}")


        val outboxRecord = OutboxRecord.create(
            orderId = event.orderId,
            idempotencyKey = event.eventId,
            eventType = OutboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = objectMapper.writeValueAsString(
                PaymentConfirmPayload(paymentId = paymentId)
            )
        )

        sageRepository.markReservationCompleteAt(event.orderId)
        outboxRepository.save(outboxRecord)
    }
}