package nuts.commerce.paymentservice.application.port.message

import java.time.Instant
import java.util.UUID

interface PaymentEventProducer {
    fun produce(message: Message)

    sealed interface Message {
        val eventId: UUID
        val occurredAt: Instant
        val paymentId: UUID
        val orderId: UUID
        val idempotencyKey: UUID

        data class PaymentApproved(
            override val eventId: UUID,
            override val occurredAt: Instant,
            override val paymentId: UUID,
            override val orderId: UUID,
            override val idempotencyKey: UUID,
            val amount: Long,
        ) : Message

        data class PaymentDeclined(
            override val eventId: UUID,
            override val occurredAt: Instant,
            override val paymentId: UUID,
            override val orderId: UUID,
            override val idempotencyKey: UUID,
            val reason: String? = null,
        ) : Message
    }
}