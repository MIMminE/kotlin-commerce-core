package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class PublishOutboxUseCase(
    private val outboxRepository: OutboxRepository,
    private val paymentEventProducer: PaymentEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${payment.outbox.batch-size:50}") private val batchSize: Int
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
            paymentEventProducer.produce(outboxInfo)
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