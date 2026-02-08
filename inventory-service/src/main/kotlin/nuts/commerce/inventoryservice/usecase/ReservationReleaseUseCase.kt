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

        // 1) 비관적 락으로 선점 + PROCESSING 마킹 / 여기서 락이 해제되어야하는데
        reservationRepository.markProcessingAndGetReservationId(orderId)

        // 2) 같은 트랜잭션에서 다시 조회
        val reservation = reservationRepository.findByOrderId(orderId)

        // 3) 재고 원복
        reservation.items.forEach { item ->
            val inv = inventoryRepository.findById(item.inventoryId)
            inv.unreserve(item.qty)
            inventoryRepository.save(inv)
        }

        // 4) 상태 전이 + 저장
        reservation.release()
        val saved = reservationRepository.save(reservation)

        // 5) outbox
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