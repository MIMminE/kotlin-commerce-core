package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.port.repository.ProductRepository
import nuts.commerce.productservice.application.port.repository.StockQuery
import org.springframework.stereotype.Component
import java.util.*

@Component
class GetProductDetailUseCase(
    private val productRepository: ProductRepository,
    private val stockQuery: StockQuery
) {

    fun execute(productId: UUID): ProductDetail {

        val product = productRepository.getActiveProduct(productId)
        return ProductDetail(
            productId = product.productId,
            productName = product.productName,
            price = product.price.amount,
            stock = stockQuery.getStockQuantity(productId),
            currency = product.price.currency
        )
    }

    data class ProductDetail(
        val productId: java.util.UUID,
        val productName: String,
        val stock: Int,
        val price: Long,
        val currency: String
    )
}