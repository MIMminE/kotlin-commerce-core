package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.OutboundEventType
import nuts.commerce.inventoryservice.event.ReservationConfirmPayload
import nuts.commerce.inventoryservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
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
class ReservationConfirmUseCase(
    private val inventoryRepository: InventoryRepository,
    private val reservationRepository: ReservationRepository,
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun execute(command: ReservationConfirmCommand): Result {

        val reservationId = command.reservationId

        val reservation = getReservation(reservationId)
        val findReservationInfo = getReservationInfo(reservationId)

        reservation.confirm()
        reservationRepository.save(reservation) // flush에 의해 트랜잭션에 반영되며 이때, 낙관적 락에 대한 예외가 발생할 수 있다.

        val payload = ReservationConfirmSuccessPayload(
            reservationItemInfoList = findReservationInfo.reservationItemInfos
        )

        val record = OutboxRecord.create(
            orderId = command.orderId,
            reservationId = reservationId,
            idempotencyKey = command.eventId,
            eventType = OutboundEventType.RESERVATION_CONFIRM,
            payload = objectMapper.writeValueAsString(payload)
        )
        outboxRepository.save(record)

        return Result(reservationId)
    }

    private fun getReservationInfo(reservationId: UUID): ReservationInfo {
        val findReservationInfo = reservationRepository.findReservationInfo(reservationId)!!
        findReservationInfo.reservationItemInfos.forEach { item ->
            val ok = inventoryRepository.commitReservedInventory(
                inventoryId = item.inventoryId,
                quantity = item.quantity
            )
            if (!ok) {
                throw IllegalStateException("Failed to commit reserved inventory for inventory ID: ${item.inventoryId}")
            }
        }
        return findReservationInfo
    }

    private fun getReservation(reservationId: UUID): Reservation = (reservationRepository.findById(reservationId)
        ?: throw IllegalArgumentException("Reservation not found: $reservationId"))

    data class Result(val reservationId: UUID)
}

data class ReservationConfirmCommand(val orderId: UUID, val eventId: UUID, val reservationId: UUID) {
    companion object {
        fun from(inboundEvent: ReservationInboundEvent): ReservationConfirmCommand {
            require(inboundEvent.eventType == InboundEventType.RESERVATION_CONFIRM)
            require(inboundEvent.payload is ReservationConfirmPayload)

            val payload = inboundEvent.payload
            return ReservationConfirmCommand(
                orderId = inboundEvent.orderId,
                eventId = inboundEvent.eventId,
                reservationId = payload.reservationId
            )
        }
    }
}