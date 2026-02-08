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
            price = Money(command.price, command.currency)
        )
        val registeredProduct = productRepository.save(product)
        return RegisteredProduct(
            productId = registeredProduct.productId,
            productName = registeredProduct.productName
        )
    }

    data class RegisterProductCommand(
        val productName: String,
        val price: Long,
        val currency: String
    )

    data class RegisteredProduct(
        val productId: UUID,
        val productName: String
    )
}