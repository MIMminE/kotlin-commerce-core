package nuts.commerce.productservice.application.adapter.repository

import nuts.commerce.productservice.application.port.repository.StockQuery
import nuts.commerce.productservice.model.exception.ProductException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RedisStockQueryAdapter(private val redis: StringRedisTemplate) : StockQuery {

    override fun getStockQuantity(productId: UUID): Int {
        val stock = redis.opsForValue().get("product_stock:$productId")
        return stock?.toInt()
            ?: throw ProductException.InvalidCommand("Stock information not found for product: $productId")
    }
}