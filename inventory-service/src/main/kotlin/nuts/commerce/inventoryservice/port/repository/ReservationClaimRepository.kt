package nuts.commerce.inventoryservice.port.repository

import java.util.*

interface ReservationClaimRepository {
    fun claimReservation(reservationId: UUID)
}
