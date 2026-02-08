package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.port.repository.ProductRepository
import nuts.commerce.productservice.port.cache.ProductStockCachePort
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetProductDetailUseCase(
    private val productRepository: ProductRepository,
    private val productStockCachePort: ProductStockCachePort
) {

    fun execute(productId: UUID): ProductDetail {

        val product = productRepository.getActiveProduct(productId)
        return ProductDetail(
            productId = product.productId,
            productName = product.productName,
            price = product.price.amount,
            stock = productStockCachePort.getStock(productId).quantity,
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