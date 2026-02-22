package nuts.commerce.orderservice.port.rest

import nuts.commerce.orderservice.model.Money
import java.util.UUID

interface ProductRestClient {
    fun getPriceSnapshots(productIds: List<UUID>):ProductPriceResponse
}

data class ProductPriceResponse(
    val productPriceSnapshot: List<ProductPriceSnapshot>,
)

data class ProductPriceSnapshot(val productId: UUID, val price: Money, val stock: Long)
