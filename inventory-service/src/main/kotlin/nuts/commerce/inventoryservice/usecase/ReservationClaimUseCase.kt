package nuts.commerce.inventoryservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.inventoryservice.port.repository.ReservationClaimRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ReservationClaimUseCase(private val reservationClaimRepository: ReservationClaimRepository) {

    @Transactional
    fun execute(orderId: UUID) {
        val reservationId = reservationClaimRepository.claimReservation(orderId)
    }
}