package nuts.commerce.inventoryservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.inventoryservice.exception.InventoryException
import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.*

@Component
class ReservationRequestUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun execute(command: ReservationRequestCommand): Result {

        val reservation =
            try {
                reservationRepository.save(
                    Reservation.create(
                        orderId = command.orderId,
                        idempotencyKey = command.eventId
                    )
                )
            } catch (ex: DataIntegrityViolationException) {
                val existing =
                    reservationRepository.findReservationIdForIdempotencyKey(command.orderId, command.eventId)
                        ?: throw ex
                return Result(existing.reservationId)
            }

        val items = command.items.sortedBy { it.productId }

        items.forEach { item ->
            val ok = inventoryRepository.reserveInventory(item.productId, item.qty)
            if (!ok) {
                throw InventoryException.InvalidCommand("Insufficient inventory for product ID: ${item.productId}")
            }
        }
        val findProductInfo = inventoryRepository.findAllByProductIdIn(items.map { it.productId })
        if (findProductInfo.size != items.size) throw InventoryException.InvalidCommand("Some product IDs are invalid.")

        val productIdToInventoryId = findProductInfo.associate { it.productId to it.inventoryId }
        val reservationItems = items.map {
            ReservationItem.create(
                reservationId = reservation.reservationId,
                inventoryId = productIdToInventoryId[it.productId]!!,
                qty = it.qty
            )
        }
        reservation.addItems(reservationItems)

        val payloadObj = mapOf(
            "items" to reservationItems.map { mapOf("inventoryId" to it.inventoryId, "quantity" to it.qty) }
        )

        val outbox = OutboxRecord.create(
            orderId = command.orderId,
            reservationId = reservation.reservationId,
            idempotencyKey = command.eventId,
            eventType = EventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObj)
        )

        outboxRepository.save(outbox)

        return Result(reservation.reservationId)
    }

    data class Result(val reservationId: UUID)
}

data class ReservationRequestCommand(
    val orderId: UUID,
    val eventId: UUID,
    val items: List<Item>
) {
    data class Item(val productId: UUID, val qty: Long)
}