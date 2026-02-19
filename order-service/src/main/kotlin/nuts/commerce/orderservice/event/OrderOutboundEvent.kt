package nuts.commerce.orderservice.event

import java.util.UUID

data class OrderOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val outboxId: UUID,
    val orderId: UUID,
    val eventType: OutboundEventType,
    val payload: OutboundPayload
)

sealed interface OutboundPayload

data class ReservationCreatePayload(
    val reservationItems: List<OutboundReservationItem>
) : OutboundPayload

data class ReservationConfirmPayload(
    val reservationId: UUID
) : OutboundPayload

data class ReservationReleasePayload(
    val reservationId: UUID
) : OutboundPayload

data class PaymentCreatePayload(
    val amount: Long,
    val currency: String = "KRW",
) : OutboundPayload

data class PaymentCreateFailedPayload(
    val reason: String
) : OutboundPayload

data class PaymentConfirmPayload(
    val paymentId: UUID
) : OutboundPayload

data class PaymentReleasePayload(
    val paymentId: UUID
) : OutboundPayload

data class OutboundReservationItem(val productId: UUID, val price: Long, val currency: String, val qty: Long)

enum class OutboundEventType {
    RESERVATION_CREATE_REQUEST,
    RESERVATION_CONFIRM_REQUEST,
    RESERVATION_RELEASE_REQUEST,
    PAYMENT_CREATE_REQUEST,
    PAYMENT_CREATE_FAILED,
    PAYMENT_CONFIRM_REQUEST,
    PAYMENT_RELEASE_REQUEST
}