package nuts.commerce.inventoryservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.OutboundEventType
import nuts.commerce.inventoryservice.event.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.ReservationRequestPayload
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationInfo
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.*

@Component
class ReservationCreateUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun execute(command: ReservationCreateCommand): ReservationRequestResult {
        // 재고 예약을 걸면서 예약 아이템 생성하는 메서드 생성
        val reservationItems = createReservationItemsWithReserve(command.items)

        val reservation = reservationRepository.save(
            Reservation.create(
                orderId = command.orderId,
                idempotencyKey = command.eventId,
                items = reservationItems
            )
        )

        try {
            val savedReservation = reservationRepository.save(reservation) // 예약 정보 저장하는 과정에서 멱등성 또는 동시성 체크가 수행된다.
            val payload = ReservationCreationSuccessPayload(
                reservationItemInfoList = reservation.items.map {
                    ReservationOutboundEvent.ReservationItem(
                        productId = it.productId,
                        qty = it.qty
                    )
                }
            )
            val outboxRecord = OutboxRecord.create(
                orderId = command.orderId,
                reservationId = savedReservation.reservationId,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payload)
            )
            outboxRepository.save(outboxRecord)
            return ReservationRequestResult(savedReservation.reservationId)

        } catch (ex: DataIntegrityViolationException) {
            val reservationInfo =
                reservationRepository.findReservationIdForIdempotencyKey(command.orderId, command.eventId)
                    ?: throw IllegalStateException("Reservation not found for idempotency key: ${command.eventId}")

            return ReservationRequestResult(reservationInfo.reservationId)
        } catch (ex: Exception) {
            val payload = ReservationCreationFailPayload(
                reason = ex.message ?: "Unknown error occurred during reservation creation"
            )
            val outboxRecord = OutboxRecord.create(
                orderId = command.orderId,
                reservationId = null,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.RESERVATION_CREATION_FAILED,
                payload = objectMapper.writeValueAsString(payload)
            )
            outboxRepository.save(outboxRecord)
            return ReservationRequestResult(reservation.reservationId)
        }
    }

    private fun createReservationItemsWithReserve(items: List<ReservationCreateCommand.Item>): List<ReservationItem> {
        items.forEach {
            if (!inventoryRepository.reserveInventory(it.productId, it.qty)) {
                throw IllegalStateException("Failed to reserve inventory for product ${it.productId}")
            }
        }    // 동시성 방어를 하면서 인벤토리 예약 [ 트랜잭션 롤백 시 롤백 ]
        return items.map {
            ReservationItem.create(
                productId = it.productId, qty = it.qty
            )
        }
    }
}

data class ReservationRequestResult(val reservationId: UUID)

data class ReservationCreateCommand(
    val orderId: UUID,
    val eventId: UUID,
    val items: List<Item>
) {
    data class Item(val productId: UUID, val qty: Long)

    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationCreateCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_REQUEST)
            require(inboundEvent.payload is ReservationRequestPayload)

            val payload = inboundEvent.payload
            return ReservationCreateCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                items = payload.requestItem.map { Item(it.productId, it.qty) }
            )
        }
    }
}