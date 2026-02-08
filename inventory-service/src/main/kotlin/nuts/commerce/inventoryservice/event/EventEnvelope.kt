package nuts.commerce.inventoryservice.event

import tools.jackson.databind.JsonNode
import java.util.UUID
import kotlin.reflect.KClass

data class EventEnvelope(
    val orderId: UUID,
    val aggregateId: UUID,
    val eventId: UUID,
    val eventType: String,
    val payload: JsonNode
)

sealed interface UseCaseCommand

interface EventHandler<C : UseCaseCommand> {
    val eventType: String
    val payloadClass: KClass<*>
    fun toCommand(envelope: EventEnvelope, payload: Any): C
}

data class ReservationCommitPayload(val reservationId: UUID)
data class ReservationReleasePayload(val reservationId: UUID)
data class ReservationRequestPayload(val items: List<Item>)
    data class Item(val productId: UUID, val qty: Long)
