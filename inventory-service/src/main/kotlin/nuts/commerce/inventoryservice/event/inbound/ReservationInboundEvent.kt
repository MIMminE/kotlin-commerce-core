package nuts.commerce.inventoryservice.event.inbound

import java.util.UUID

data class ReservationInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,
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
    RESERVATION_CREATE,
    RESERVATION_CONFIRM,
    RESERVATION_RELEASE
}