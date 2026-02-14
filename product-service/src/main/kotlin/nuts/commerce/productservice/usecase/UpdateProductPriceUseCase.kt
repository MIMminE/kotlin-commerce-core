package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.port.message.ProduceResult
import nuts.commerce.productservice.port.message.ProductEventProducer
import nuts.commerce.productservice.port.message.ProductUpdateInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class UpdateProductPriceUseCase(
    private val productRepository: ProductRepository,
    private val productEventProducer: ProductEventProducer,
    private val logger: Logger = LoggerFactory.getLogger(UpdateProductPriceUseCase::class.java)
) {

    @Transactional
    fun execute(principalName: String, productId: UUID, price: Long, currency: String) {
        val product = productRepository.getProduct(productId) ?: throw IllegalArgumentException("Product not found")
        product.updatePrice(Money(price, currency))

        val produceResult = productEventProducer.produce(ProductUpdateInfo(principalName, productId, price, currency))

        when (produceResult) {
            is ProduceResult.Success -> {
                logger.info("Successfully produced product update event for productId: ${produceResult.productId} by ${produceResult.requestPrincipalName}")
            }

            is ProduceResult.Failure -> {
                logger.error("Failed to produce product update event for productId: ${produceResult.productId} by ${produceResult.requestPrincipalName}. Reason: ${produceResult.reason}")
                throw RuntimeException("Failed to produce product update event: ${produceResult.reason}")
            }
        }
    }
}