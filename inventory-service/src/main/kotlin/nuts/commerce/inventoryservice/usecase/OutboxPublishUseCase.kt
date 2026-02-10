package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.InventoryEvent
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
    @Qualifier("outboxUpdateExecutor")
    private val outboxUpdateExecutor: Executor,
    @Value($$"${inventory.outbox.batch-size:50}")
    private val batchSize: Int

) {
    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(
            batchSize = batchSize,
            lockedBy = "Nuts-Worker"
        )

        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.claimOutboxInfo.forEach { record ->
            val inventoryEvent = InventoryEvent.create(
                outboxId = record.outboxId,
                orderId = record.orderId,
                reservationId = record.reservationId,
                eventType = record.eventType,
                payload = record.payload
            )
            eventProducer.produce(inventoryEvent)
                .whenCompleteAsync(
                    { result, ex ->
                        when {
                            ex != null -> {
                                outboxRepository.markFailed(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Success -> {
                                outboxRepository.markPublished(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }

                            result is ProduceResult.Failure -> {
                                outboxRepository.markFailed(
                                    outboxId = record.outboxId,
                                    lockedBy = "Nuts-Worker"
                                )
                            }
                        }
                    }, outboxUpdateExecutor
                )
        }
    }
}