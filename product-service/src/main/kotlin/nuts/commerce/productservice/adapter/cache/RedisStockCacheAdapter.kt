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

    private val log = LoggerFactory.getLogger(RedisStockCacheAdapter::class.java)

    override fun getStock(productId: UUID): Long {
        return redisTemplate.opsForValue().get(productId)
            ?: throw IllegalStateException("Stock not found in cache for productId: $productId")
    }

    override fun getStocks(productIds: List<UUID>): Map<UUID, Long> {
        val stocks: List<Long?> = redisTemplate.opsForValue().multiGet(productIds).orEmpty()

        if (stocks.size != productIds.size) {
            throw IllegalStateException("multiGet size mismatch. ids=${productIds.size}, stocks=${stocks.size}")
        }

        val missing = productIds.zip(stocks)
            .filter { (_, stock) -> stock == null }
            .map { (id, _) -> id }

        if (missing.isNotEmpty()) {
            throw IllegalStateException("Stock not found in cache for productIds: $missing")
        }

        return productIds.zip(stocks).associate { (id, stock) -> id to stock!! }
    }

    override fun saveStock(productId: UUID, stock: Long) {
        log.info("Saving stock to cache. productId={}, stock={}", productId, stock)
        redisTemplate.opsForValue().set(productId, stock)
    }

    override fun plusStock(productId: UUID, plusStock: Long) {

        if (plusStock < 0) {
            throw IllegalArgumentException("plusStock must be non-negative")
        }
        val existed = redisTemplate.hasKey(productId) == true
        if (!existed) {
            throw IllegalStateException("Stock not found in cache for productId: $productId")
        }
        log.info("Incrementing stock in cache. productId={}, plusStock={}", productId, plusStock)
        redisTemplate.opsForValue().increment(productId, plusStock)
    }

    override fun minusStock(productId: UUID, minusStock: Long) {

        if (minusStock < 0) {
            throw IllegalArgumentException("minusStock must be non-negative")
        }
        val existed = redisTemplate.hasKey(productId) == true
        if (!existed) {
            throw IllegalStateException("Stock not found in cache for productId: $productId")
        }
        log.info("Decrementing stock in cache. productId={}, minusStock={}", productId, minusStock)
        redisTemplate.opsForValue().decrement(productId, minusStock)
    }
}