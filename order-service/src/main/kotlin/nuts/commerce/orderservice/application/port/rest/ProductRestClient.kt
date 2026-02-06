package nuts.commerce.orderservice.application.port.rest

interface ProductRestClient {
    fun getPriceSnapshots(productIds: List<String>): List<ProductPriceSnapshot>
}

data class ProductPriceSnapshot(val productId: String, val price: Long, val currency: String)
