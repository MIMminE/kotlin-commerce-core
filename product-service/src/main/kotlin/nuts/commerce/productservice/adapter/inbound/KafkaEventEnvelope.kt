package nuts.commerce.productservice.adapter.inbound

import tools.jackson.databind.JsonNode
import java.util.UUID

data class KafkaEventEnvelope(
    val eventId: UUID,
    val orderId: UUID,
    val eventType: ListenEventType,
    val payload: JsonNode
)

enum class ListenEventType {
    RESERVATION_CREATION_SUCCEEDED,
    RESERVATION_CONFIRM_FAILED,
    RESERVATION_RELEASE_SUCCEEDED
}

data class CreationSucceededPayload(
    val reservationId: UUID,
    val reservationItems: List<ReservationItem>
) {
    data class ReservationItem(
        val productId: UUID,
        val qty: Long
    )
}