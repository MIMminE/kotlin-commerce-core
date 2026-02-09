package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.ReservationItem
import java.util.UUID

interface ReservationItemRepository {
    fun save(items: List<ReservationItem>): List<ReservationItem>
    fun findByReservationId(reservationId: UUID): List<ReservationItem>
}