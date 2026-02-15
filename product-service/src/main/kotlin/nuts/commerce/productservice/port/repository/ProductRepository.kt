package nuts.commerce.productservice.port.repository

import nuts.commerce.productservice.model.Money
import nuts.commerce.productservice.model.Product
import java.util.UUID

interface ProductRepository {
    fun save(product: Product): UUID
    fun getAllProductInfo(): List<ProductInfo>
    fun getProduct(productId: UUID): ProductInfo?
}

data class ProductInfo(
    val productId: UUID,
    val productName: String,
    val price: Money,
)