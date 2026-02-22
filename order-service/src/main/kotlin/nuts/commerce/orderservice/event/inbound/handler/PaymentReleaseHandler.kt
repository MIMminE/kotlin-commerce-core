package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.PaymentReleaseSuccessPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayloadReservation
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class PaymentReleaseHandler(
    private val orderRepository: OrderRepository,
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : InboundEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.PAYMENT_RELEASE

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val eventId = orderInboundEvent.eventId
        val orderId = orderInboundEvent.orderId
        val payload = orderInboundEvent.payload as PaymentReleaseSuccessPayload

        val reservationId = sageRepository.findSageInfoByOrderId(orderId)?.let {
            it.reservationId ?: throw IllegalStateException("reservation id is null for order id: $orderId")
        } ?: throw IllegalStateException("sage info not found for order id: $orderId")

        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            idempotencyKey = eventId,
            eventType = OutboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = objectMapper.writeValueAsString(
                ReservationReleasePayloadReservation(reservationId = reservationId)
            )
        )

        orderRepository.updateStatus(orderId, OrderStatus.PAYING, OrderStatus.PAYMENT_FAILED)
        sageRepository.markReservationReleaseAt(orderId, payload.reason)
        outboxRepository.save(outboxRecord)
    }
}