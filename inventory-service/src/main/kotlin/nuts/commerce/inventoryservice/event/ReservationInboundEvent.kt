package nuts.commerce.inventoryservice.event

import java.util.UUID

data class ReservationInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: InboundEventType,
    val payload: InboundPayload
)

sealed interface InboundPayload

data class ReservationRequestPayload(
    val requestItem: List<ReservationItem>
) : InboundPayload

data class ReservationConfirmPayload(
    val reservationId: UUID
) : InboundPayload

data class ReservationReleasePayload(
    val reservationId: UUID
) : InboundPayload

data class ReservationItem(val productId: UUID, val qty: Long)

enum class InboundEventType {
    RESERVATION_REQUEST,
    RESERVATION_CONFIRM,
    RESERVATION_RELEASE
}