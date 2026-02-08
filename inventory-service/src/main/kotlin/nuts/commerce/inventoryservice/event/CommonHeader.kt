package nuts.commerce.inventoryservice.event

import java.util.UUID

data class CommonHeader(val orderId: UUID, val aggregateId: UUID, val eventId: UUID)