package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.*
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.*

@Component
class ReservationReleaseUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: ReservationReleaseCommand): ReservationReleaseResult {

        val reservation = (reservationRepository.findById(command.reservationId)
            ?: throw IllegalStateException("Reservation not found for ID: ${command.reservationId}"))

        try {
            reservation.release()
            reservationRepository.save(reservation)
            reservationRepository.findReservationInfo(command.reservationId)!!
                .reservationItemInfos.forEach {
                    val ok = inventoryRepository.releaseReservedInventory(
                        productId = it.productId,
                        quantity = it.quantity
                    )
                    if (!ok) {
                        throw IllegalStateException("Failed to release reserved inventory for product ID: ${it.productId}")
                    }
                }

            val payload = ReservationReleaseSuccessPayload(
                reservationItemInfoList = reservation.items.map {
                    ReservationOutboundEvent.ReservationItem(
                        productId = it.productId,
                        qty = it.qty
                    )
                }
            )

            val outboxRecord = OutboxRecord.create(
                orderId = command.orderId,
                reservationId = reservation.reservationId,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.RESERVATION_RELEASE,
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

data class ReservationReleaseResult(val reservationId: UUID)
data class ReservationReleaseCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID) {
    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationReleaseCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_RELEASE)
            require(inboundEvent.payload is ReservationReleasePayload)

            val payload = inboundEvent.payload
            return ReservationReleaseCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                reservationId = payload.reservationId
            )
        }
    }
}