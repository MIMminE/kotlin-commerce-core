package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.port.repository.ProductInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetProductsUseCase(private val productRepository: ProductRepository) {

    fun execute(): List<ProductInfo> {
        return productRepository.getAllProductInfo()
    }
}