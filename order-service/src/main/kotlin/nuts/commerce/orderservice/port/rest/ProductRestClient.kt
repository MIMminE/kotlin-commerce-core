package nuts.commerce.orderservice.port.rest

import java.util.UUID

interface ProductRestClient {
    fun getPriceSnapshots(productIds: List<UUID>):ProductPriceResponse
}

data class ProductPriceResponse(
    val productPriceSnapshot: List<ProductPriceSnapshot>,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class ProductPriceSnapshot(val productId: UUID, val price: Long, val currency: String)
