package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent
import nuts.commerce.orderservice.event.inbound.PaymentCreationSuccessPayload
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationConfirmPayload
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.port.repository.OutboxRepository
import nuts.commerce.orderservice.port.repository.SageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class PaymentCreateSuccessHandler(
    private val sageRepository: SageRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : OrderEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.PAYMENT_CREATION_SUCCEEDED

    @Transactional
    override fun handle(orderInboundEvent: OrderInboundEvent) {
        val orderId = orderInboundEvent.orderId
        val eventId = orderInboundEvent.eventId
        val payload = orderInboundEvent.payload as PaymentCreationSuccessPayload
        val reservationId = sageRepository.findSageInfoByOrderId(orderId)?.let {
            it.reservationId ?: throw IllegalStateException("reservation id is null for order id: $orderId")
        } ?: throw IllegalStateException("sage info not found for order id: $orderId")

        val outboxRecord = OutboxRecord.Companion.create(
            orderId = orderId,
            idempotencyKey = eventId,
            eventType = OutboundEventType.RESERVATION_CONFIRM_REQUEST,
            payload = objectMapper.writeValueAsString(
                ReservationConfirmPayload(
                    reservationId = reservationId
                )
            )
        )

        sageRepository.markReservationCompleteAt(orderId)
        outboxRepository.save(outboxRecord)

    }
}