package nuts.commerce.productservice.port.cache

import java.util.UUID

interface StockCachePort {
    fun getStock(productId: UUID): Long
    fun saveStock(productId: UUID, stock: Long)
    fun updateStock(productId: UUID, updateStock: Long)
}