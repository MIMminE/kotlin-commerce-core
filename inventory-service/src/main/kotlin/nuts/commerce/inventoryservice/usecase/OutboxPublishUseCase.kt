package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.message.QuantityUpdateEventProducer
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val producer: QuantityUpdateEventProducer,
    private val txManager: PlatformTransactionManager,
    private val objectMapper: ObjectMapper,
    @Value("\${inventory.outbox.batch-size:50}")
    private val batchSize: Int
) {
    fun execute(){

        val claimOutboxIds = outboxRepository.claimAndLockBatchIds(
            batchSize = batchSize,
            lockedBy = "Nuts-Worker"
        )



    }
}