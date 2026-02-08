package nuts.commerce.productservice.adapter.cache

import nuts.commerce.productservice.exception.ProductException
import nuts.commerce.productservice.port.cache.ProductStockCachePort
import nuts.commerce.productservice.port.cache.StockQuantity
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class RedisProductStockCachePortAdapter(private val redis: StringRedisTemplate) : ProductStockCachePort {

    override fun getStock(productId: UUID): StockQuantity {
        val stock = redis.opsForValue().get("product_stock:$productId")
            ?: throw ProductException.InvalidCommand("Stock information not found for product: $productId")

        return StockQuantity(
            quantity = stock.toLong(),
            updatedAt = Instant.now()
        )
    }
}