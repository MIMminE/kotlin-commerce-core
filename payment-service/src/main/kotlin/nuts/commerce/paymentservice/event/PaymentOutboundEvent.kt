package nuts.commerce.paymentservice.event

import java.util.UUID

class PaymentOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val outboxId: UUID,
    val orderId: UUID,
    val paymentId: UUID,
    val eventType: OutboundEventType,
    val payload: OutboundPayload
)

sealed interface OutboundPayload

class PaymentCreationSuccessPayload(
    val paymentProvider: String
) : OutboundPayload

class PaymentCreationFailPayload(
    val reason: String
) : OutboundPayload

class PaymentConfirmSuccessPayload(
    val paymentProvider: String,
    val providerPaymentId: UUID

) : OutboundPayload

class PaymentReleaseSuccessPayload(
    val reason: String
) : OutboundPayload

enum class OutboundEventType {
    PAYMENT_CREATION_SUCCEEDED,
    PAYMENT_CREATION_FAILED,
    PAYMENT_CONFIRM,
    PAYMENT_RELEASE,
}