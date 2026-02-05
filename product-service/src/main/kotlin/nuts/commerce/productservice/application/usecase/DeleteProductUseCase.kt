package nuts.commerce.productservice.application.usecase

import nuts.commerce.productservice.application.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeleteProductUseCase(private val productRepository: ProductRepository) {

    fun execute(productId: UUID): Result {
        val product = productRepository.findById(productId)
        product.delete()
        productRepository.save(product)
        return Result(product.productId)
    }

    data class Result(val productId: UUID)
}
