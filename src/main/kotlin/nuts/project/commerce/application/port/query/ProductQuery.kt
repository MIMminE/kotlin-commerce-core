package nuts.project.commerce.application.port.query

import nuts.project.commerce.application.port.dto.ProductPrice
import nuts.project.commerce.domain.product.Product
import java.util.UUID

interface ProductQuery {
    fun getProduct(productId : UUID) : Product?
    fun getUnitPrices(productIds: List<UUID>): List<ProductPrice>
}