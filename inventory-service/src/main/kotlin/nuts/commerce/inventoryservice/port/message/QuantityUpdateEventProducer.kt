package nuts.commerce.inventoryservice.port.message

import java.util.UUID
import java.util.concurrent.CompletableFuture


interface QuantityUpdateEventProducer {
    fun produce(inventoryId: UUID, event: QuantityUpdateEvent): CompletableFuture<Unit>
}

data class QuantityUpdateEvent(val productId: UUID, val quantity: Long)