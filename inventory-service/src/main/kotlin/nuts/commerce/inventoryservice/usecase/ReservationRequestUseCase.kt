package nuts.commerce.inventoryservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.OutboundEventType
import nuts.commerce.inventoryservice.event.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.ReservationRequestPayload
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationInfo
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
    fun execute(command: ReservationRequestCommand): ReservationRequestResult {

        val createResult = createReservation(command)

        if (createResult.isNewlyCreated) {
            val payload = ReservationCreationSuccessPayload(
                reservationItemInfoList =
            )

            val outbox = OutboxRecord.create(
                orderId = command.orderId,
                reservationId = reservation.reservationId,
                idempotencyKey = command.eventId,
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payload)
            )

            outboxRepository.save(outbox)


        }

    }


    private fun createReservation(command: ReservationRequestCommand): CreateReservationResult {
        val reservation = try {
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

            return CreateReservationResult(
                reservation = existing,
                isNewlyCreated = false
            )
        }
    }
}


data class ReservationRequestResult(val reservationId: UUID)
data class CreateReservationResult(
    val reservationInfo: ReservationInfo,
    val isNewlyCreated: Boolean
)

data class ReservationRequestCommand(
    val orderId: UUID,
    val eventId: UUID,
    val items: List<Item>
) {
    data class Item(val productId: UUID, val qty: Long)

    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationRequestCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_REQUEST)
            require(inboundEvent.payload is ReservationRequestPayload)

            val payload = inboundEvent.payload
            return ReservationRequestCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                items = payload.requestItem.map { Item(it.productId, it.qty) }
            )
        }
    }
}