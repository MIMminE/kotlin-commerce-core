package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.model.EventType
import java.util.UUID

sealed interface PaymentEvent {
    val eventId: UUID
    val outboxId: UUID
    val orderId: UUID
    val paymentId: UUID
    val eventType: EventType
}

data class PaymentCreationEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_CREATION_SUCCEEDED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val paymentProvider: String
    )
}

data class PaymentCreationFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_CREATION_FAILED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val reason: String
    )
}

data class PaymentConfirmedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_CONFIRM_SUCCEEDED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val paymentProvider: String,
        val providerPaymentId: UUID
    )
}

data class PaymentConfirmationFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_CONFIRM_FAILED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val paymentProvider: String,
        val providerPaymentId: UUID,
        val reason: String
    )
}

data class PaymentReleasedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_RELEASE_SUCCEEDED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val paymentProvider: String,
        val providerPaymentId: UUID
    )
}

data class PaymentReleaseFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val paymentId: UUID,
    override val eventType: EventType = EventType.PAYMENT_RELEASE_FAILED,
    val payload: Payload
) : PaymentEvent {
    data class Payload(
        val paymentProvider: String,
        val providerPaymentId: UUID,
        val reason: String
    )
}