package nuts.commerce.productservice.adapter.cache

import nuts.commerce.productservice.port.cache.StockCachePort
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RedisStockCacheAdapter(
    private val redisTemplate: RedisTemplate<UUID, Long>
) : StockCachePort {

    private val logger = LoggerFactory.getLogger(RedisStockCacheAdapter::class.java)

    override fun getStock(productId: UUID): Long {
        return redisTemplate.opsForValue().get(productId)
            ?: throw IllegalStateException("Stock not found in cache for productId: $productId")
    }
}