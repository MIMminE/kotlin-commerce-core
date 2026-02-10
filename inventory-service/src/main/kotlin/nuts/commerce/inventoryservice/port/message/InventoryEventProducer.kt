package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.model.EventType
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface InventoryEventProducer {
    fun produce(inventoryEvent: InventoryEvent): CompletableFuture<ProduceResult>
}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}