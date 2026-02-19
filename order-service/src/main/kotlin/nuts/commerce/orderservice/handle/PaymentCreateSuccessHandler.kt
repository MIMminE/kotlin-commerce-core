package nuts.commerce.orderservice.handle

import nuts.commerce.orderservice.event.InboundEventType
import nuts.commerce.orderservice.event.OrderInboundEvent
import nuts.commerce.orderservice.event.OutboundEventType
import nuts.commerce.orderservice.event.PaymentCreationSuccessPayload
import nuts.commerce.orderservice.event.PaymentReleaseSuccessPayload
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
class PaymentCreateSuccessHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun handle(event: OrderInboundEvent) {

        require(event.eventType == InboundEventType.PAYMENT_CREATION_SUCCEEDED)
        require(event.payload is PaymentCreationSuccessPayload)

        val reservationId = sageRepository.findSageInfoByOrderId(event.orderId)?.let {
            it.reservationId ?: throw IllegalStateException("reservation id is null for order id: ${event.orderId}")
        } ?: throw IllegalStateException("sage info not found for order id: ${event.orderId}")

        val outboxRecord = OutboxRecord.Companion.create(
            orderId = event.orderId,
            idempotencyKey = event.eventId,
            eventType = OutboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = objectMapper.writeValueAsString(
                ReservationConfirmPayload(
                    reservationId = reservationId
                )
            )
        )

        sageRepository.markReservationCompleteAt(event.orderId)
        outboxRepository.save(outboxRecord)
    }
}