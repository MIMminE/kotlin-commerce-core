package nuts.commerce.productservice.adapter.cache

import nuts.commerce.productservice.port.cache.StockCachePort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RedisStockCacheAdapter(
    private val redisTemplate: RedisTemplate<UUID, Long>
) : StockCachePort {

    override fun getStock(productId: UUID): Long {
        return redisTemplate.opsForValue().get(productId)
            ?: throw IllegalStateException("Stock not found in cache for productId: $productId")
    }

    override fun saveStock(productId: UUID, expectStock: Long, updateStock: Long) {
        redisTemplate.execute { connection ->
            connection.watch(productId.toString().toByteArray())
            val currentStock = redisTemplate.opsForValue().get(productId)

            if (currentStock == null) {
                redisTemplate.opsForValue().set(productId, updateStock)
            } else {
                if (currentStock != expectStock) {
                    throw IllegalStateException("Expected stock does not match current stock for productId: $productId")
                }
                connection.multi()
                redisTemplate.opsForValue().set(productId, updateStock)
                connection.exec()
            }
        }
    }
}