package nuts.commerce.productservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.productservice.model.ProductStatus
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ActivateProductUseCase(private val productRepository: ProductRepository) {

    @Transactional
    fun execute(productId: UUID): Result {
        val product = productRepository.findById(productId)
        val beforeStatus = product.status
        product.activate()
        productRepository.save(product)
        val afterStatus = product.status
        return Result(product.productId, beforeStatus, afterStatus)
    }

    data class Result(val productId: UUID, val beforeStatus: ProductStatus, val afterStatus: ProductStatus)
}
