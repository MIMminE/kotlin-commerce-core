package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.ReservationItemInfo
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

data class ReservationConfirmSucceededEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_CONFIRM_SUCCEEDED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reservationItems: List<ReservationItemInfo>)
}

data class ReservationConfirmFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_CONFIRM_FAILED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reason: String, val reservationItems: List<ReservationItemInfo>)
}

data class ReservationReleaseSucceededEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_RELEASE_SUCCEEDED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reservationItems: List<ReservationItemInfo>)
}

data class ReservationReleaseFailedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_RELEASE_FAILED,
    val payload: Payload
) : InventoryEvent {
    data class Payload(val reason: String, val reservationItems: List<ReservationItemInfo>)
}