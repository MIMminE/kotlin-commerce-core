package nuts.commerce.productservice.port.cache

import java.util.UUID

interface StockCachePort {
    fun getStock(productId: UUID): Long
}