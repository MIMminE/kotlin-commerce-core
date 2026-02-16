package nuts.commerce.inventoryservice.event

import nuts.commerce.inventoryservice.port.repository.ReservationInfo
import java.util.UUID

class ReservationOutboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val reservationId: UUID,
    val eventType: OutboundEventType,
    val payload: OutboundPayload
)

sealed interface OutboundPayload

class ReservationCreationSuccessPayload(
    val reservationItemInfoList: List<ReservationInfo.ReservationItemInfo>,
) : OutboundPayload

class ReservationCreationFailPayload(
    val reason: String
) : OutboundPayload

class ReservationConfirmSuccessPayload(
    val reservationItemInfoList: List<ReservationInfo.ReservationItemInfo>
) : OutboundPayload

class ReservationReleaseSuccessPayload(
    val reservationItemInfoList: List<ReservationInfo.ReservationItemInfo>
) : OutboundPayload


enum class OutboundEventType {
    RESERVATION_CREATION_SUCCEEDED,
    RESERVATION_CREATION_FAILED,
    RESERVATION_CONFIRM,
    RESERVATION_RELEASE,
}