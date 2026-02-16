package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.ReservationItemInfo
import nuts.commerce.inventoryservice.event.ReservationReleasePayload
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.port.repository.ReservationInfo
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

        val reservation = getReservation(reservationId)
        val findReservationInfo = getReservationInfo(reservationId)

        reservation.release()
        reservationRepository.save(reservation)

        ReservationConfirmSuccessPayload(
            reservationItemInfoList = findReservationInfo.reservationItemInfos.map {
                ReservationItemInfo(
                    inventoryId = it.inventoryId,
                    qty = it.quantity
                )
            }
        )
        val payloadObj = mapOf("reservationItems" to findReservationInfo)

        val outbox = OutboxRecord.create(
            orderId = command.orderId,
            reservationId = reservationId,
            idempotencyKey = command.eventId,
            eventType = EventType.RESERVATION_RELEASE,
            payload = objectMapper.writeValueAsString(payloadObj)
        )
        outboxRepository.save(outbox)

        return Result(reservationId)
    }

    private fun getReservationInfo(reservationId: UUID): ReservationInfo {
        val findReservationInfo = reservationRepository.findReservationInfo(reservationId)!!
        findReservationInfo.reservationItemInfos.forEach { item ->
            val ok = inventoryRepository.releaseReservedInventory(
                inventoryId = item.inventoryId,
                quantity = item.quantity
            )
            if (!ok) {
                throw IllegalStateException("Failed to release reserved inventory for inventory ID: ${item.inventoryId}")
            }
        }
        return findReservationInfo
    }

    private fun getReservation(reservationId: UUID): Reservation = (reservationRepository.findById(reservationId)
        ?: throw IllegalArgumentException("Reservation not found: $reservationId"))

    data class Result(val reservationId: UUID)
}

data class ReservationReleaseCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID) {
    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationReleaseCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_RELEASE)
            require(inboundEvent.payload is ReservationReleasePayload)

            val payload = inboundEvent.payload
            return ReservationReleaseCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                reservationId = payload.reservationId
            )
        }
    }
}