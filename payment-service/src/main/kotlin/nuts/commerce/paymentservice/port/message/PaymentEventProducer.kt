package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.model.OutboxInfo
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface PaymentEventProducer {
    fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult>
}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}