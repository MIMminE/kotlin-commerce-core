package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface PaymentEventProducer {
    fun produce(paymentOutboundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult>
}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}