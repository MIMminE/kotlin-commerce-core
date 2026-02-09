package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Reservation
import java.util.UUID

interface ReservationRepository {
    fun save(reservation: Reservation): Reservation
    fun findById(reservationId: UUID): Reservation?
    fun findReservationIdForIdempotencyKey(orderId: UUID, idempotencyKey: UUID): Reservation? // TODO 수정 필요
    fun findReservationInfo(reservationId: UUID): ReservationInfo?
}

data class ReservationInfo(
    val reservationId: UUID,
    val items: List<Item>
) {

    data class Item(
        val inventoryId: UUID,
        val quantity: Long
    )
}