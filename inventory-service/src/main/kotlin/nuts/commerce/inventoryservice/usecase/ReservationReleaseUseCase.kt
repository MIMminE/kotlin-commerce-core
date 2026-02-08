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

        reservation.release()
        reservation.items.forEach { item ->
            val inv = inventoryRepository.findById(item.inventoryId)
            inv.unreserve(item.qty)
            inventoryRepository.save(inv)
        }

        val outbox = OutboxRecord.createWithPayload(
            reservationId = reservationId,
            eventType = EventType.RESERVATION_RELEASED,
            payloadObj = "reservationId" to reservationId,
            objectMapper = objectMapper
        )
        outboxRepository.save(outbox)

        return Result(reservationId)
    }

    data class Result(val reservationId: UUID)

    private data class ReservationReleasedPayload(val reservationId: UUID, val items: List<Item>) {
        data class Item(val inventoryId: UUID, val qty: Long)
    }
}

data class ReservationReleaseCommand(val reservationId: UUID)