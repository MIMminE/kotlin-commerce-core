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

    override fun saveStock(productId: UUID, stock: Long) {
        redisTemplate.opsForValue().set(productId, stock)
    }

    override fun updateStock(productId: UUID, updateStock: Long) {
        val existed = redisTemplate.hasKey(productId) == true
        if (!existed) {
            throw IllegalStateException("Stock not found in cache for productId: $productId")
        }
        redisTemplate.opsForValue().set(productId, updateStock)
    }
}