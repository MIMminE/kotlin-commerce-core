package nuts.commerce.inventoryservice.event

import java.util.UUID

class ReservationOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val outboxId: UUID,
    val orderId: UUID,
    val reservationId: UUID?,
    val eventType: OutboundEventType,
    val payload: OutboundPayload
) {
    data class ReservationItem(
        val productId: UUID,
        val qty: Long
    )
}

sealed interface OutboundPayload

class ReservationCreationSuccessPayload(
    val reservationItemInfoList: List<ReservationOutboundEvent.ReservationItem>,
) : OutboundPayload

class ReservationCreationFailPayload(
    val reason: String
) : OutboundPayload

class ReservationConfirmSuccessPayload(
    val reservationItemInfoList: List<ReservationOutboundEvent.ReservationItem>
) : OutboundPayload

class ReservationReleaseSuccessPayload(
    val reservationItemInfoList: List<ReservationOutboundEvent.ReservationItem>
) : OutboundPayload

enum class OutboundEventType {
    RESERVATION_CREATION_SUCCEEDED,
    RESERVATION_CREATION_FAILED,
    RESERVATION_CONFIRM,
    RESERVATION_RELEASE,
}