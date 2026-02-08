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

    override fun findById(reservationId: UUID): Reservation {
        return reservationJpa.findById(reservationId).orElseThrow {
            IllegalArgumentException("Reservation not found: reservationId=$reservationId")
        }
    }
}

interface ReservationJpa : JpaRepository<Reservation, UUID> {
}