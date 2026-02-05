package nuts.commerce.inventoryservice.application.port.message

import java.util.UUID

interface InventoryCachePublisher {
    fun publish(inventoryId: UUID, productId: UUID, quantity: Long)
}
