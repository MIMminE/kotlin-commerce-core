package nuts.commerce.inventoryservice.event.inbound

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class ReservationInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,

    @field:JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "eventType"
    )
    @field:JsonSubTypes(
        JsonSubTypes.Type(value = ReservationCreatePayload::class, name = "RESERVATION_CREATE_REQUEST"),
        JsonSubTypes.Type(value = ReservationConfirmPayload::class, name = "RESERVATION_CONFIRM_REQUEST"),
        JsonSubTypes.Type(value = ReservationReleasePayload::class, name = "RESERVATION_RELEASE_REQUEST")
    )
    val payload: InboundPayload
)

sealed interface InboundPayload

data class ReservationCreatePayload(
    val requestItem: List<InboundReservationItem>
) : InboundPayload

data class ReservationConfirmPayload(
    val reservationId: UUID
) : InboundPayload

data class ReservationReleasePayload(
    val reservationId: UUID
) : InboundPayload

data class InboundReservationItem(val productId: UUID, val qty: Long)

enum class InboundEventType {
    RESERVATION_CREATE_REQUEST,
    RESERVATION_CONFIRM_REQUEST,
    RESERVATION_RELEASE_REQUEST
}