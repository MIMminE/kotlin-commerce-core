package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.OutboxClaimRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class OutboxClaimUseCase(private val outboxClaimRepository: OutboxClaimRepository) {

    @Transactional
    fun execute(limit: Int): List<UUID> {
        return outboxClaimRepository.claimOutboxRecords(limit)
    }
}