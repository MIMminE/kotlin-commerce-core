package nuts.commerce.inventoryservice.event

import java.util.UUID

data class ReservationRequestPayload(val items: List<Item>) {
    data class Item(val productId: UUID, val qty: Long)
}