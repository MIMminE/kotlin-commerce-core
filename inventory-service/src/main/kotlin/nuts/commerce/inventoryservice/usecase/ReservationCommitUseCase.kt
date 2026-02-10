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
class ReservationCommitUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: ReservationCommitCommand): Result {

        val reservationId = command.reservationId
        val reservation = reservationRepository.findById(reservationId)
            ?: throw IllegalArgumentException("Reservation not found: $reservationId")

        reservation.commit()
        reservationRepository.save(reservation) // flush에 의해 트랜잭션에 반영되며 이때, 낙관적 락에 대한 예외가 발생할 수 있다.

        val findReservationInfo = reservationRepository.findReservationInfo(reservationId)!!
        findReservationInfo.items.forEach { item ->
            val ok = inventoryRepository.commitReservedInventory(
                inventoryId = item.inventoryId,
                quantity = item.quantity
            )
            if (!ok) {
                throw IllegalStateException("Failed to commit reserved inventory for inventory ID: ${item.inventoryId}")
            }
        }
        val payloadObj = mapOf("reservationInfo" to findReservationInfo)

        val record = OutboxRecord.create(
            orderId = command.orderId,
            reservationId = reservationId,
            idempotencyKey = command.eventId,
            eventType = EventType.RESERVATION_COMMITTED,
            payload = objectMapper.writeValueAsString(payloadObj)
        )
        outboxRepository.save(record)

        return Result(reservationId)
    }

    data class Result(val reservationId: UUID)
}

data class ReservationCommitCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID)