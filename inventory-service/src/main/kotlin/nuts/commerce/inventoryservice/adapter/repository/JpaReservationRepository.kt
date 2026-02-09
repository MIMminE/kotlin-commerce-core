package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.port.repository.ReservationInfo
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaReservationRepository(private val reservationJpa: ReservationJpa) : ReservationRepository {
    override fun save(reservation: Reservation): Reservation {
        return reservationJpa.saveAndFlush(reservation)
    }

    override fun findById(reservationId: UUID): Reservation? {
        return reservationJpa.findById(reservationId).orElse(null)
    }

    override fun findByOrderIdAndIdempotencyKey(
        orderId: UUID,
        idempotencyKey: UUID
    ): Reservation? {
        return reservationJpa.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey)
    }

    override fun findReservationInfo(reservationId: UUID): ReservationInfo? {
        return reservationJpa.fetchJoinedById(reservationId)?.let { reservation ->
            return ReservationInfo(
                reservationId = reservation.reservationId,
                items = reservation.items.map { item ->
                    ReservationInfo.Item(
                        inventoryId = item.inventoryId,
                        quantity = item.qty
                    )
                }
            )
        }
    }
}

interface ReservationJpa : JpaRepository<Reservation, UUID> {
    fun findByOrderIdAndIdempotencyKey(orderId: UUID, idempotencyKey: UUID): Reservation?


    @Query(
        """
        select r from Reservation r
        left join fetch r.items
        where r.reservationId = :reservationId
        """
    )
    fun fetchJoinedById(reservationId: UUID): Reservation?
}