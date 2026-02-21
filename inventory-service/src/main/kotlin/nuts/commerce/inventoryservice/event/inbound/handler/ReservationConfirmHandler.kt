package nuts.commerce.inventoryservice.event.inbound.handler

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.ReservationConfirmPayload
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class ReservationConfirmHandler(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : ReservationEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_CONFIRM

    @Transactional
    override fun handle(reservationInboundEvent: ReservationInboundEvent) {
        val eventId = reservationInboundEvent.eventId
        val orderId = reservationInboundEvent.orderId
        val payload = reservationInboundEvent.payload as ReservationConfirmPayload
        val reservation = reservationRepository.findById(payload.reservationId)
            ?: throw IllegalArgumentException("Invalid reservation id: ${payload.reservationId}")

        try {
            reservation.confirm()
            reservationRepository.save(reservation)
            reservationRepository.findReservationInfo(payload.reservationId)!!
                .reservationItemInfos.forEach {
                    if (!inventoryRepository.confirmReservedInventory(
                            productId = it.productId,
                            quantity = it.quantity
                        )
                    ) {
                        throw IllegalStateException("Failed to confirm reserved inventory for product ID: ${it.productId}")
                    }
                }

            val payload = ReservationConfirmSuccessPayload(
                reservationId = reservation.reservationId,
                reservationItemInfoList = reservation.items.map {
                    ReservationOutboundEvent.ReservationItem(
                        productId = it.productId,
                        qty = it.qty
                    )
                }
            )

            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                reservationId = payload.reservationId,
                idempotencyKey = eventId,
                eventType = OutboundEventType.RESERVATION_CONFIRM,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outboxRecord)

        } catch (ex: DataIntegrityViolationException) {
            reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
                ?: throw IllegalStateException("Reservation not found for idempotency key: $eventId")
        }

    }
}