package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.ProduceResult
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val eventProducer: InventoryEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${inventory.outbox.batch-size:50}") private val batchSize: Int

) {
    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(
            batchSize = batchSize,
            lockedBy = "Nuts-Worker"
        )

        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.claimOutboxInfo.forEach { outboxInfo ->
            eventProducer.produce(outboxInfo)
                .whenCompleteAsync(
                    { result, ex ->
                        when {
                            ex != null -> {
                                outboxRepository.markFailed(
                                    outboxId = outboxInfo.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Success -> {
                                outboxRepository.markPublished(
                                    outboxId = outboxInfo.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Failure -> {
                                outboxRepository.markFailed(
                                    outboxId = outboxInfo.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }
                        }
                    }, outboxUpdateExecutor
                )
        }
    }
}