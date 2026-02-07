package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Reservation
import java.util.UUID

interface ReservationRepository {
    fun save(reservation : Reservation): Reservation
    fun findByOrderId(orderId: UUID): Reservation
}