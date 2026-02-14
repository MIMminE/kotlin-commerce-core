package nuts.commerce.inventoryservice.adapter.inbound

import tools.jackson.databind.JsonNode
import java.util.UUID

data class KafkaEventEnvelope(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: ListenEventType,
    val payload: JsonNode
)

enum class ListenEventType {
    RESERVATION_REQUEST,
    RESERVATION_CONFIRM,
    RESERVATION_RELEASE
}

data class RequestPayload(
    val items: List<Item>
) {
    data class Item(
        val productId: UUID,
        val qty: Long
    )
}

data class CommitPayload(
    val reservationId: UUID
)

data class ReleasePayload(
    val reservationId: UUID
)