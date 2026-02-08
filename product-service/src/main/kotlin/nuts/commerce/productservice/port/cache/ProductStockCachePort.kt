package nuts.commerce.productservice.port.cache

import java.time.Instant
import java.util.UUID

interface ProductStockCachePort {
    fun getStock(productId: UUID): StockQuantity
}

data class StockQuantity (val quantity: Long, val updatedAt: Instant)