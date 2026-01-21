package nuts.project.commerce.application.port.inventory

import java.util.UUID

interface InventoryUpdatePort {
    fun reserve(orderId: UUID, productId: UUID, quantity: Int, ttlSeconds: Long)
    fun confirm(orderId: UUID)
    fun release(orderId: UUID)
}