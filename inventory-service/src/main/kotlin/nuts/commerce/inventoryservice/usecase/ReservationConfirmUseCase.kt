package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.OutboundEventType
import nuts.commerce.inventoryservice.event.ReservationConfirmPayload
import nuts.commerce.inventoryservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.ReservationOutboundEvent
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxRecord
import org.springframework.dao.DataIntegrityViolationException
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ReservationConfirmUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: ReservationConfirmCommand): ReservationReleaseResult {

        val reservation = reservationRepository.findById(command.reservationId)
            ?: throw IllegalStateException("Reservation not found for ID: ${command.reservationId}")
        try {
            reservation.confirm()
            reservationRepository.save(reservation)
            reservationRepository.findReservationInfo(command.reservationId)!!
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
                reservationItemInfoList = reservation.items.map {
                    ReservationOutboundEvent.ReservationItem(
                        productId = it.productId,
                        qty = it.qty
                    )
                }
            )

            val outboxRecord = OutboxRecord.create(
                orderId = command.orderId,
                reservationId = command.reservationId,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.RESERVATION_CONFIRM,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outboxRecord)
            return ReservationReleaseResult(command.reservationId)

        } catch (ex: DataIntegrityViolationException) {
            reservationRepository.findReservationIdForIdempotencyKey(command.orderId, command.eventId)
                ?: throw IllegalStateException("Reservation not found for idempotency key: ${command.eventId}")
            return ReservationReleaseResult(command.reservationId)
        }
    }
}

data class ReservationConfirmResult(val reservationId: UUID)
data class ReservationConfirmCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID) {
    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationConfirmCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_CONFIRM)
            require(inboundEvent.payload is ReservationConfirmPayload)

            val payload = inboundEvent.payload
            return ReservationConfirmCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                reservationId = payload.reservationId
            )
        }
    }
}