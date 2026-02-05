package nuts.commerce.productservice.model.exception

import nuts.commerce.productservice.model.domain.Product
import java.util.UUID

sealed class ProductException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class InvalidTransition(
        val productId: UUID?,
        val from: Product.ProductStatus,
        val to: Product.ProductStatus
    ) : ProductException("invalid transition: $from -> $to (productId=$productId)")


    class InvalidCommand(message: String) : ProductException(message)
}