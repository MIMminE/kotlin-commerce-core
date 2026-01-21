package nuts.project.commerce.application.port.product

import nuts.project.commerce.domain.product.StockHandlingPolicy
import java.util.UUID

interface ProductQueryPort {
    fun getUnitPrices(productIds: List<UUID>): List<ProductPrice>
    fun getInventoryPolicies(productIds: List<UUID>): List<ProductInventoryPolicy>
}

data class ProductPrice(val productId: UUID, val unitPrice: Long)
data class ProductInventoryPolicy(val productId: UUID, val stockHandlingPolicy: StockHandlingPolicy)