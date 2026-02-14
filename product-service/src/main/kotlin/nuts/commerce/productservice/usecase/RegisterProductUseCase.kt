package nuts.commerce.productservice.usecase

import jakarta.transaction.Transactional
import nuts.commerce.productservice.port.repository.ProductRepository
import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RegisterProductUseCase(private val productRepository: ProductRepository) {

    @Transactional
    fun execute(command: RegisterProductCommand): RegisteredProduct {
        val product = Product.create(
            productName = command.productName,
            price = Money(command.price, command.currency),
            idempotencyKey = command.idempotencyKey
        )
        val productId = productRepository.save(product)
        return RegisteredProduct(
            productId = productId,
            productName = command.productName
        )
    }

    data class RegisterProductCommand(
        val idempotencyKey: UUID,
        val productName: String,
        val price: Long,
        val currency: String
    )

    data class RegisteredProduct(
        val productId: UUID,
        val productName: String
    )
}