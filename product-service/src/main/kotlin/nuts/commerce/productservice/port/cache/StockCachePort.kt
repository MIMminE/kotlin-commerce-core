package nuts.commerce.productservice.port.cache

import java.util.UUID

interface StockCachePort {
    fun getStock(productId: UUID): Long
    fun getStocks(productIds: List<UUID>): Map<UUID, Long>
    fun saveStock(productId: UUID, stock: Long)
    fun plusStock(productId: UUID, plusStock: Long)
    fun minusStock(productId: UUID, minusStock: Long)
}