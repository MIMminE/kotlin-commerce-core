package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxEventType
import nuts.commerce.inventoryservice.model.OutboxRecord
import tools.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.NoSuchElementException
import java.util.UUID

@Component
class ReservationReleaseUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(orderId: UUID): Result {
        val reservation = try {
            reservationRepository.findByOrderId(orderId)
        } catch (e: NoSuchElementException) {
            throw NoSuchElementException("reservation not found for orderId: $orderId")
        }

        reservation.items.forEach { item ->
            val inv = inventoryRepository.findById(item.inventoryId)
            inv.unreserve(item.qty)
            inventoryRepository.save(inv)
        }

        reservation.release()
        val saved = reservationRepository.save(reservation)

        val payload = ReservationReleasedPayload(
            reservationId = saved.reservationId,
            items = saved.items.map { ReservationReleasedPayload.Item(it.inventoryId, it.qty) }
        )

        val outbox = OutboxRecord.createWithPayload(
            reservationId = saved.reservationId,
            eventType = OutboxEventType.RESERVATION_RELEASED,
            payloadObj = payload,
            objectMapper = objectMapper
        )
        outboxRepository.save(outbox)

        return Result(saved.reservationId)
    }

    data class Result(val reservationId: UUID)

    private data class ReservationReleasedPayload(val reservationId: UUID, val items: List<Item>) {
        data class Item(val inventoryId: UUID, val qty: Long)
    }
}