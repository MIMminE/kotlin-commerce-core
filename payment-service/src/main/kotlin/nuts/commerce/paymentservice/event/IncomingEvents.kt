package nuts.commerce.paymentservice.event

import java.time.Instant
import java.util.UUID

sealed interface IncomingEvent {
    val eventId: UUID
    val occurredAt: Instant
    val paymentId: UUID
    val orderId: UUID
    val idempotencyKey: UUID

    data class PaymentRequested(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val amount: Long
    ) : IncomingEvent

    data class PaymentConfirmed(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val amount: Long
    ) : IncomingEvent

    data class PaymentCanceled(
        override val eventId: UUID,
        override val occurredAt: Instant,
        override val paymentId: UUID,
        override val orderId: UUID,
        override val idempotencyKey: UUID,
        val reason: String? = null
    ) : IncomingEvent
}

enum class IncomingEventType {
    PAYMENT_REQUESTED,
    PAYMENT_CONFIRMED,
    PAYMENT_CANCELED
}
