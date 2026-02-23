package nuts.commerce.productservice.testutil

import nuts.commerce.productservice.port.cache.StockCachePort
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryStockCache : StockCachePort {
    private val map = ConcurrentHashMap<UUID, Long>()

    override fun getStock(productId: UUID): Long {
        return map[productId] ?: throw IllegalStateException("Stock not found in cache for productId: $productId")
    }

    override fun getStocks(productIds: List<UUID>): Map<UUID, Long> {
        val missing = productIds.filter { !map.containsKey(it) }
        if (missing.isNotEmpty()) {
            throw IllegalStateException("Stock not found in cache for productIds: $missing")
        }
        return productIds.associateWith { map[it]!! }
    }

    override fun saveStock(productId: UUID, stock: Long) {
        map[productId] = stock
    }

    override fun plusStock(productId: UUID, plusStock: Long) {
        if (plusStock < 0) {
            throw IllegalArgumentException("plusStock must be non-negative")
        }
        val existed = map.containsKey(productId)
        if (!existed) {
            throw IllegalStateException("Stock not found in cache for productId: $productId")
        }
        val newStock = map[productId]!! + plusStock
        if (newStock < 0) {
            throw IllegalStateException("Stock cannot be negative after plus operation. productId: $productId, newStock: $newStock")
        }
        map[productId] = newStock
    }

    override fun minusStock(productId: UUID, minusStock: Long) {
        if (minusStock < 0) {
            throw IllegalArgumentException("minusStock must be non-negative")
        }
        val existed = map.containsKey(productId)
        if (!existed) {
            throw IllegalStateException("Stock not found in cache for productId: $productId")
        }
        val newStock = map[productId]!! - minusStock
        if (newStock < 0) {
            throw IllegalStateException("Stock cannot be negative after minus operation. productId: $productId, newStock: $newStock")
        }
        map[productId] = newStock
    }

    fun clear() {
        map.clear()
    }
}

