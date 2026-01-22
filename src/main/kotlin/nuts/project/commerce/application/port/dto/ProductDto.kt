package nuts.project.commerce.application.port.dto

import nuts.project.commerce.domain.product.StockHandlingPolicy
import java.util.UUID

data class ProductPrice(val productId: UUID, val unitPrice: Long)
data class ProductInventoryPolicy(val productId: UUID, val stockHandlingPolicy: StockHandlingPolicy)
