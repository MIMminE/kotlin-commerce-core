package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxRecord
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ReservationReleaseUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: ReservationReleaseCommand): Result {

        val reservationId = command.reservationId
        val reservation = reservationRepository.findById(reservationId)
            ?: throw IllegalArgumentException("Reservation not found: $reservationId")

        reservation.release()
        reservationRepository.save(reservation)

        val findReservationInfo = reservationRepository.findReservationInfo(reservationId)!!
        findReservationInfo.items.forEach { item ->
            val ok = inventoryRepository.releaseReservedInventory(
                inventoryId = item.inventoryId,
                quantity = item.quantity
            )
            if (!ok) {
                throw IllegalStateException("Failed to release reserved inventory for inventory ID: ${item.inventoryId}")
            }
        }
        val payloadObj = mapOf("reservationInfo" to findReservationInfo)
        val outbox = OutboxRecord.create(
            orderId = command.orderId,
            reservationId = reservationId,
            idempotencyKey = command.eventId,
            eventType = EventType.RESERVATION_RELEASE_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObj)
        )
        outboxRepository.save(outbox)

        return Result(reservationId)
    }

    data class Result(val reservationId: UUID)
}

data class ReservationReleaseCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID)