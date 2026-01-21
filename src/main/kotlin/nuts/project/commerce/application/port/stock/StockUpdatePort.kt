package nuts.project.commerce.application.port.stock

import java.util.UUID

interface StockUpdatePort {
    fun reserve(orderId: UUID, productId: UUID, quantity: Int, ttlSeconds: Long)
    fun confirm(orderId: UUID)
    fun release(orderId: UUID)
}