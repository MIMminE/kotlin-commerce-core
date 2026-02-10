package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.model.EventType
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface PaymentEventProducer {
    fun produce(paymentEvent: PaymentEvent): CompletableFuture<>
}

@ConsistentCopyVisibility
data class PaymentEvent internal constructor(
    val eventId: UUID,
    val outboxId: UUID,
    val orderId: UUID,
    val paymentId: UUID,
    val eventType: EventType,
    val payload: String
) {
    companion object {
        fun create(
            orderId: UUID,
            outboxId: UUID,
            paymentId: UUID,
            eventType: EventType,
            payload: String,
            eventId: UUID = UUID.randomUUID(),
        ): PaymentEvent =
            PaymentEvent(
                eventId = eventId,
                outboxId = outboxId,
                orderId = orderId,
                paymentId = paymentId,
                eventType = eventType,
                payload = payload
            )
    }
}


sealed interface PaymentEvent2 {
    val eventId: UUID
    val outboxId: UUID
    val orderId: UUID
    val paymentId: UUID
    val eventType: EventType

}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}