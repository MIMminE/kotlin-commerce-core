package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.EventType
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxRecord
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.collections.mapOf

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

        reservation.commit()
        reservation.items.forEach { item ->
            val inv = inventoryRepository.findById(item.inventoryId)
            inv.processReserved(item.qty)
            inventoryRepository.save(inv)
        }


        val record = OutboxRecord.createWithPayload(
            reservationId = reservationId,
            eventType = EventType.RESERVATION_COMMITTED,
            payloadObj = "reservationId" to reservationId,
            objectMapper = objectMapper
        )
        outboxRepository.save(record)

        return Result(reservationId)
    }

    data class Result(val reservationId: UUID)

}

data class ReservationCommitCommand(val reservationId: UUID)