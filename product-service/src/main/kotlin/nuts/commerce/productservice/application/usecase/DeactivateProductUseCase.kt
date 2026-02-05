package nuts.commerce.productservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.productservice.application.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeactivateProductUseCase(private val productRepository: ProductRepository) {

    @Transactional
    fun execute(productId: UUID): Result {
        val product = productRepository.findById(productId)
        product.deactivate()
        productRepository.save(product)
        return Result(product.productId)
    }

    data class Result(val productId: UUID)
}
