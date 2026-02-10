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
    override val eventId: UUID,
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID = UUID.randomUUID(),
    override val eventType: EventType = EventType.RESERVATION_CREATION,
    val createdReservationItems: List<CreatedReservationItem>
) : InventoryEvent {
    data class CreatedReservationItem(
        val inventoryId: UUID,
        val quantity: Long
    )
}

data class ReservationCommittedEvent(
    override val eventId: UUID,
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_COMMITTED,
    val commitedReservationItems: List<CommitedReservationItem>
) : InventoryEvent {
    data class CommitedReservationItem(
        val inventoryId: UUID,
        val quantity: Long
    )
}

data class ReservationReleasedEvent(
    override val eventId: UUID,
    override val outboxId: UUID,
    override val orderId: UUID,
    override val reservationId: UUID,
    override val eventType: EventType = EventType.RESERVATION_RELEASED,
    val releasedReservationItems: List<ReleasedReservationItem>
) : InventoryEvent {
    data class ReleasedReservationItem(
        val inventoryId: UUID,
        val quantity: Long
    )
}