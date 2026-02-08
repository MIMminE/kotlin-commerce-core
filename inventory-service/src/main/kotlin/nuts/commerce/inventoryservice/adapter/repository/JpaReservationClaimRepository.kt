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

    override fun claimReservation(reservationId: UUID) {
        val updated = reservationJpa.claimByOrderId(
            reservationId = reservationId,
            fromStatus = ReservationStatus.CREATED,
            toStatus = ReservationStatus.PROCESSING
        )

        reservationJpa.existsById(reservationId)


        if (updated != 1) {
            throw IllegalStateException("Reservation is not claimable: reservationId=$reservationId")
        }
    }
}

interface ReservationClaimJpa : JpaRepository<Reservation, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Reservation r
           set r.status = :toStatus
         where r.reservationId = :reservationId
           and r.status = :fromStatus
    """
    )
    fun claimByOrderId(
        @Param("reservationId") reservationId: UUID,
        @Param("fromStatus") fromStatus: ReservationStatus,
        @Param("toStatus") toStatus: ReservationStatus
    ): Int
}