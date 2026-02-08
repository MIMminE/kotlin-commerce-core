package nuts.commerce.inventoryservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.inventoryservice.port.repository.ReservationClaimRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ReservationClaimUseCase(private val reservationClaimRepository: ReservationClaimRepository) {

    @Transactional
    fun execute(reservationId: UUID) : ReservationClaimResult{
        reservationClaimRepository.claimReservation(reservationId)
    }
}

data class ReservationClaimResult(
    val reservationId: UUID,
    val alreadyClaimed: Boolean
)