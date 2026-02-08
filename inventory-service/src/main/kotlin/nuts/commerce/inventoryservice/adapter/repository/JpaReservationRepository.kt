package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaReservationRepository(
    private val reservationJpa: ReservationJpa,
) : ReservationRepository {

    override fun save(reservation: Reservation): Reservation = reservationJpa.save(reservation)

    override fun findByOrderId(orderId: UUID): Reservation =
        reservationJpa.findByOrderId(orderId)
            ?: throw NoSuchElementException("Reservation not found for orderId: $orderId")

}

interface ReservationJpa : JpaRepository<Reservation, UUID> {
    fun findByOrderId(orderId: UUID): Reservation?
}