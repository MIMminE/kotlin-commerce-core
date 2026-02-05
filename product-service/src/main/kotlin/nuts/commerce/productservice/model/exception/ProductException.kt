package nuts.commerce.productservice.model.exception

import nuts.commerce.productservice.model.domain.ProductStatus
import java.util.*

sealed class ProductException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class InvalidTransition(
        val productId: UUID?,
        val from: ProductStatus,
        val to: ProductStatus
    ) : ProductException("invalid transition: $from -> $to (productId=$productId)")

    class InvalidCommand(message: String) : ProductException(message)
}