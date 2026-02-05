package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetProductsUseCase(private val productRepository: ProductRepository) {

    fun execute(): List<ProductSummary> {

        val products = productRepository.getActiveProducts()
        return products.map {
            ProductSummary(
                productId = it.productId,
                productName = it.productName,
            )
        }
    }

    data class ProductSummary(
        val productId: UUID,
        val productName: String,
    )
}