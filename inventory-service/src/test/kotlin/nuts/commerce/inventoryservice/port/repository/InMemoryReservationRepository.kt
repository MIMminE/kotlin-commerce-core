package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Reservation
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryReservationRepository : ReservationRepository {
    private val byId: MutableMap<UUID, Reservation> = ConcurrentHashMap()
    private val byOrderId: MutableMap<UUID, Reservation> = ConcurrentHashMap()

    fun clear() {
        byId.clear()
        byOrderId.clear()
    }

    override fun save(reservation: Reservation): Reservation {
        byId[reservation.reservationId] = reservation
        byOrderId[reservation.orderId] = reservation
        return reservation
    }

    override fun findByOrderId(orderId: UUID): Reservation {
        return byOrderId[orderId] ?: throw NoSuchElementException("Reservation not found for orderId: $orderId")
    }
}

