package nuts.commerce.productservice.port.message

import java.util.UUID
import java.util.concurrent.CompletableFuture

interface ProductEventProducer {
    fun produce(productUpdateInfo: ProductUpdateInfo): ProduceResult
}

data class ProductUpdateInfo(
    val requestPrincipalName: String,
    val productId: UUID,
    val price: Long,
    val currency: String,
)

sealed interface ProduceResult {
    data class Success(val requestPrincipalName: String, val productId: UUID) : ProduceResult
    data class Failure(val requestPrincipalName: String, val productId: UUID, val reason: String) :
        ProduceResult
}