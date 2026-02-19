package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.port.message.OrderEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import nuts.commerce.orderservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.*
import java.util.concurrent.Executor

@Component
class PublishOrderOutboxUseCase(
    private val outboxRepository: OutboxRepository,
    private val eventProducer: OrderEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${order.outbox.batch-size:50}") private val batchSize: Int,
) {

    fun publishPendingOutboxMessages() {
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