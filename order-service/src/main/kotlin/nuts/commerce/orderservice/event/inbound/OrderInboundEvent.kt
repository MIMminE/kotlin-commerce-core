package nuts.commerce.orderservice.event.inbound

import java.util.UUID

data class OrderInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,
    val payload: InboundPayload
)

sealed interface InboundPayload

data class ReservationCreationSucceededPayload(
    val reservationItemInfoList: List<InboundReservationItem>
) : InboundPayload

data class ReservationCreationFailedPayload(
    val reason: String
) : InboundPayload

data class ReservationConfirmSuccessPayload(
    val reservationId: UUID,
    val reservationItemInfoList: List<InboundReservationItem>
) : InboundPayload

data class PaymentCreationSuccessPayload(
    val paymentProvider: String
) : InboundPayload

data class PaymentCreationFailedPayload(
    val reason: String
) : InboundPayload

data class PaymentConfirmSuccessPayload(
    val paymentProvider: String,
    val providerPaymentId: UUID
) : InboundPayload

data class PaymentReleaseSuccessPayload(
    val reason: String
) : InboundPayload

data class InboundReservationItem(
    val productId: UUID,
    val qty: Long
)

enum class InboundEventType {
    RESERVATION_CREATION_SUCCEEDED,
    RESERVATION_CREATION_FAILED,
    RESERVATION_CONFIRM,
    PAYMENT_CREATION_SUCCEEDED,
    PAYMENT_CREATION_FAILED,
    PAYMENT_CONFIRM,
    PAYMENT_RELEASE,
}