package nuts.commerce.paymentservice.event

import java.time.Instant
import java.util.UUID


sealed interface OutgoingEvent {
    val eventId: UUID
    val occurredAt: Instant
    val paymentId: UUID
    val orderId: UUID
    val idempotencyKey: UUID


    data class PaymentSessionCreated(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val amount: Long,
        val paymentSessionId: UUID
    ) : OutgoingEvent


    data class PaymentConfirmed(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val amount: Long,
        val confirmationId: UUID
    ) : OutgoingEvent


    data class PaymentFailed(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val failureCode: String? = null,
        val reason: String? = null
    ) : OutgoingEvent
}

enum class OutgoingEventType {
    PAYMENT_SESSION_CREATED,
    PAYMENT_CONFIRMED,
    PAYMENT_FAILED
}