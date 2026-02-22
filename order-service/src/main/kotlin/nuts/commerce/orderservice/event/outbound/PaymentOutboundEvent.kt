package nuts.commerce.orderservice.event.outbound

import java.util.UUID

class PaymentOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val outboxId: UUID,
    val orderId: UUID,
    val eventType: OutboundEventType,
    val payload: PaymentOutboundPayload
)

sealed interface PaymentOutboundPayload

data class PaymentCreatePayload(
    val amount: Long,
    val currency: String = "KRW",
) : PaymentOutboundPayload

data class PaymentCreateFailedPayload(
    val reason: String
) : PaymentOutboundPayload

data class PaymentConfirmPayload(
    val paymentId: UUID
) : PaymentOutboundPayload

data class PaymentReleasePayload(
    val paymentId: UUID
) : PaymentOutboundPayload