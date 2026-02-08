package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationStatus
import nuts.commerce.inventoryservice.port.repository.ReservationClaimRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaReservationClaimRepository(
    private val reservationJpa: ReservationClaimJpa
) : ReservationClaimRepository {

    override fun claimReservation(orderId: UUID): UUID? {
        val updated = reservationJpa.claimByOrderId(
            orderId = orderId,
            fromStatus = ReservationStatus.CREATED,
            toStatus = ReservationStatus.PROCESSING
        )

        if (updated != 1) {
            throw IllegalStateException("Reservation is not claimable: orderId=$orderId")
        }

        return reservationJpa.findReservationIdByOrderId(orderId)
            ?: throw NoSuchElementException("Reservation not found for orderId: $orderId")
    }
}

interface ReservationClaimJpa : JpaRepository<Reservation, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Reservation r
           set r.status = :toStatus
         where r.orderId = :orderId
           and r.status = :fromStatus
    """
    )
    fun claimByOrderId(
        @Param("orderId") orderId: UUID,
        @Param("fromStatus") fromStatus: ReservationStatus,
        @Param("toStatus") toStatus: ReservationStatus
    ): Int

    @Query("select r.reservationId from Reservation r where r.orderId = :orderId")
    fun findReservationIdByOrderId(@Param("orderId") orderId: UUID): UUID?
}