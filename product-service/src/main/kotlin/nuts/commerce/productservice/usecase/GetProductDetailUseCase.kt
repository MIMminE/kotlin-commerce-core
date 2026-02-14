package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetProductDetailUseCase(
    private val productRepository: ProductRepository,
    private val stockChaPort: StockCachePort
) {

    fun execute(productId: UUID): ProductDetail {

        val product = productRepository.getProduct(productId) ?: throw IllegalArgumentException("Product not found")
        return ProductDetail(
            productId = product.productId,
            productName = product.productName,
            price = product.price.amount,
            stock = stockChaPort.getStock(productId),
            currency = product.price.currency
        )
    }

    data class ProductDetail(
        val productId: UUID,
        val productName: String,
        val stock: Long,
        val price: Long,
        val currency: String
    )
}