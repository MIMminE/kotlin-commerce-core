package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.model.EventType
import java.util.UUID

sealed interface InventoryEvent {
    val eventId: UUID
    val outboxId: UUID
    val orderId: UUID
    val reservationId: UUID
    val eventType: EventType
}

data class ReservationCreationEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_CREATION_SUCCEEDED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reservationItems: List<ReservationItemInfo>)
}

data class ReservationCreationFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_CREATION_FAILED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reason: String)
}

data class ReservationConfirmEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_CONFIRM,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reservationItems: List<ReservationItemInfo>)
}

data class ReservationReleaseEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_RELEASE,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reservationItems: List<ReservationItemInfo>)
}

data class ReservationItemInfo(
    val inventoryId: UUID,
    val qty: Long
)