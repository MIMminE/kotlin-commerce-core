package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxEventType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.NoSuchElementException
import java.util.UUID

@Component
class ReservationRequestUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: Command): Result {
        val existing = tryFindByOrderIdOrNull(command.orderId)
        if (existing != null && existing.idempotencyKey == command.idempotencyKey) {
            return Result(existing.reservationId)
        }

        if (command.items.isEmpty()) throw IllegalArgumentException("items required")

        command.items
            .groupBy { it.inventoryId }
            .forEach { (inventoryId, itemsForInventory) ->
                val totalQty = itemsForInventory.sumOf { it.qty }
                val inv = inventoryRepository.findById(inventoryId)
                inv.reserve(totalQty)
                inventoryRepository.save(inv)
            }

        val reservation = Reservation.create(
            orderId = command.orderId,
            idempotencyKey = command.idempotencyKey
        )

        val reservationItems = command.items.map { itDto ->
            ReservationItem.create(reservation = reservation, inventoryId = itDto.inventoryId, qty = itDto.qty)
        }
        reservation.addItems(reservationItems)

        val savedReservation = try {
            reservationRepository.save(reservation)
        } catch (ex: DataIntegrityViolationException) {
            val found = tryFindByOrderIdOrNull(command.orderId)
                ?: throw ex
            return Result(found.reservationId)
        }

        val payloadObj = ReservationCreatedPayload(
            reservationId = savedReservation.reservationId,
            items = savedReservation.items.map { ReservationCreatedPayload.Item(it.inventoryId, it.qty) }
        )
        val outbox = OutboxRecord.createWithPayload(
            reservationId = savedReservation.reservationId,
            eventType = OutboxEventType.RESERVATION_CREATED,
            payloadObj = payloadObj,
            objectMapper = objectMapper
        )
        outboxRepository.save(outbox)

        return Result(savedReservation.reservationId)
    }

    private fun tryFindByOrderIdOrNull(orderId: UUID): Reservation? = try {
        reservationRepository.findByOrderId(orderId)
    } catch (e: NoSuchElementException) {
        null
    }

    data class Command(
        val orderId: UUID,
        val idempotencyKey: UUID,
        val items: List<Item>
    ) {
        data class Item(val inventoryId: UUID, val qty: Long)
    }

    data class Result(val reservationId: UUID)

    private data class ReservationCreatedPayload(val reservationId: UUID, val items: List<Item>) {
        data class Item(val inventoryId: UUID, val qty: Long)
    }
}

data class ReservationRequestPayload(val items: List<Item>) {
    data class Item(val productId: UUID, val qty: Long)
}