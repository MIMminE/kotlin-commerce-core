package nuts.commerce.orderservice.event.outbound

import java.util.UUID

data class ReservationOutboundEvent(
    val eventId: UUID = UUID.randomUUID(),
    val outboxId: UUID,
    val orderId: UUID,
    val eventType: OutboundEventType,
    val payload: ReservationOutboundPayload
)

sealed interface ReservationOutboundPayload

data class ReservationCreatePayloadReservation(
    val reservationItems: List<OutboundReservationItem>
) : ReservationOutboundPayload

data class ReservationConfirmPayloadReservation(
    val reservationId: UUID
) : ReservationOutboundPayload

data class ReservationReleasePayloadReservation(
    val reservationId: UUID
) : ReservationOutboundPayload

data class OutboundReservationItem(val productId: UUID, val price: Long, val currency: String, val qty: Long)
