package nuts.commerce.paymentservice.event

import java.util.UUID

class PaymentInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,
    val payload: InboundPayload
)

sealed interface InboundPayload

data class PaymentCreatePayload(
    val amount: Long,
    val currency: String = "KRW",
) : InboundPayload

data class PaymentConfirmPayload(
    val paymentId: UUID
) : InboundPayload

data class PaymentReleasePayload(
    val paymentId: UUID
) : InboundPayload

enum class InboundEventType {
    PAYMENT_CREATE,
    PAYMENT_CONFIRM,
    PAYMENT_RELEASE
}