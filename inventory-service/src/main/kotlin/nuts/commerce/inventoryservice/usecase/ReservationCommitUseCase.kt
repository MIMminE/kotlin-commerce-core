package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxEventType
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
    fun execute(orderId: UUID): Result {
        val reservation = reservationRepository.findByOrderId(orderId)

        reservation.commit()

        val saved = reservationRepository.save(reservation)

        saved.items.forEach { item ->
            val inv = inventoryRepository.findById(item.inventoryId)
            inv.processReserved(item.qty)
            inventoryRepository.save(inv)
        }

        val payload = ReservationCommittedPayload(
            reservationId = saved.reservationId,
            items = saved.items.map { ReservationCommittedPayload.Item(it.inventoryId, it.qty) }
        )

        val outbox = OutboxRecord.createWithPayload(
            reservationId = saved.reservationId,
            eventType = OutboxEventType.RESERVATION_COMMITTED,
            payloadObj = payload,
            objectMapper = objectMapper
        )
        outboxRepository.save(outbox)

        return Result(saved.reservationId)
    }

    data class Command(val orderId: UUID)
    data class Result(val reservationId: UUID)

    private data class ReservationCommittedPayload(val reservationId: UUID, val items: List<Item>) {
        data class Item(val inventoryId: UUID, val qty: Long)
    }
}