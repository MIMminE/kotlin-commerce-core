package nuts.commerce.productservice.event

import java.util.UUID

data class ProductInboundEvent(
    val eventId: UUID,
    val orderId: UUID,
    val reservationId: UUID,
    val eventType: InboundEventType,
    val payload: InboundPayload
) {
    data class ReservationItem(
        val productId: UUID,
        val qty: Long
    )
}

sealed interface InboundPayload

data class ReservationCreationSuccessPayload(
    val reservationItemInfoList: List<ProductInboundEvent.ReservationItem>
) : InboundPayload

data class ReservationConfirmSuccessPayload(
    val reservationItemInfoList: List<ProductInboundEvent.ReservationItem>
) : InboundPayload

enum class InboundEventType {
    RESERVATION_CREATION_SUCCEEDED,
    RESERVATION_RELEASE
}