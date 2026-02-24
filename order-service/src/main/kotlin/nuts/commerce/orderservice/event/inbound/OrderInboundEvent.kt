package nuts.commerce.orderservice.event.inbound

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class OrderInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,

    @field:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "eventType"
    )
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = ReservationCreationSucceededPayload::class, name = "RESERVATION_CREATION_SUCCEEDED"),
        JsonSubTypes.Type(value = ReservationCreationFailedPayload::class, name = "RESERVATION_CREATION_FAILED"),
        JsonSubTypes.Type(value = ReservationConfirmSuccessPayload::class, name = "RESERVATION_CONFIRM"),
        JsonSubTypes.Type(value = ReservationReleaseSuccessPayload::class, name = "RESERVATION_RELEASE"),
        JsonSubTypes.Type(value = PaymentCreationSuccessPayload::class, name = "PAYMENT_CREATION_SUCCEEDED"),
        JsonSubTypes.Type(value = PaymentCreationFailedPayload::class, name = "PAYMENT_CREATION_FAILED"),
        JsonSubTypes.Type(value = PaymentConfirmSuccessPayload::class, name = "PAYMENT_CONFIRM"),
        JsonSubTypes.Type(value = PaymentReleaseSuccessPayload::class, name = "PAYMENT_RELEASE"),
    )
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

data class ReservationReleaseSuccessPayload(
    val reservationId: UUID,
    val reservationItemInfoList: List<InboundReservationItem>,
    val reason: String
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
    RESERVATION_RELEASE,
    PAYMENT_CREATION_SUCCEEDED,
    PAYMENT_CREATION_FAILED,
    PAYMENT_CONFIRM,
    PAYMENT_RELEASE,
}