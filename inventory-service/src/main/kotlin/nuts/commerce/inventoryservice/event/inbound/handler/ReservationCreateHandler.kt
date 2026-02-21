package nuts.commerce.inventoryservice.event.inbound.handler

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.InboundReservationItem
import nuts.commerce.inventoryservice.event.inbound.ReservationCreatePayload
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class ReservationCreateHandler(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : ReservationEventHandler {
    override val supportType: InboundEventType
        get() = InboundEventType.RESERVATION_CREATE

    @Transactional
    override fun handle(reservationInboundEvent: ReservationInboundEvent) {
        val eventId = reservationInboundEvent.eventId
        val orderId = reservationInboundEvent.orderId
        val payload = reservationInboundEvent.payload as ReservationCreatePayload

        val reservationItems = createReservationItemsWithReserve(payload.requestItem)
        val reservation = reservationRepository.save(
            Reservation.create(
                orderId = orderId,
                idempotencyKey = eventId,
                items = reservationItems
            )
        )

        try {
            val savedReservation = reservationRepository.save(reservation) // 예약 정보 저장하는 과정에서 멱등성 또는 동시성 체크가 수행된다.
            val payload = ReservationCreationSuccessPayload(
                reservationId = savedReservation.reservationId,
                reservationItemInfoList = reservation.items.map {
                    ReservationOutboundEvent.ReservationItem(
                        productId = it.productId,
                        qty = it.qty
                    )
                }
            )
            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                reservationId = savedReservation.reservationId,
                idempotencyKey = eventId,
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payload)
            )
            outboxRepository.save(outboxRecord)

        } catch (ex: DataIntegrityViolationException) {
            val reservationInfo =
                reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
                    ?: throw IllegalStateException("Reservation not found for idempotency key: ${eventId}")

        } catch (ex: Exception) {
            val payload = ReservationCreationFailPayload(
                reason = ex.message ?: "Unknown error occurred during reservation creation"
            )
            val outboxRecord = OutboxRecord.create(
                orderId = orderId,
                reservationId = null,
                idempotencyKey = eventId,
                eventType = OutboundEventType.RESERVATION_CREATION_FAILED,
                payload = objectMapper.writeValueAsString(payload)
            )
            outboxRepository.save(outboxRecord)
        }
    }

    private fun createReservationItemsWithReserve(items: List<InboundReservationItem>): List<ReservationItem> {
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