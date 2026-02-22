package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetProductsUseCase(
    private val productRepository: ProductRepository,
    private val stockCachePort: StockCachePort
) {

    fun execute(): List<ProductSummary> {
        val allProductInfo = productRepository.getAllProductInfo()
        if (allProductInfo.isEmpty()) throw IllegalStateException("상품 정보가 존재하지 않습니다.")
        val productIds = allProductInfo.map { it.productId }
        val stocks = stockCachePort.getStocks(productIds)

        return allProductInfo.map { p ->
            val stock =
                stocks[p.productId] ?: throw IllegalStateException("재고 정보를 가져오는데 실패했습니다. productId: ${p.productId}")
            ProductSummary(
                productId = p.productId,
                productName = p.productName,
                price = p.price,
                stock = stock
            )
        }
    }
}

data class ProductSummary(
    val productId: UUID,
    val productName: String,
    val price: Money,
    val stock: Long
)