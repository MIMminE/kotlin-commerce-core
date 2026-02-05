package nuts.commerce.productservice.application.port.repository

import nuts.commerce.productservice.model.domain.Product
import java.util.UUID

interface ProductRepository {
    fun save(product: Product): Product
    fun getActiveProducts(): List<Product>
    fun getActiveProduct(productId: UUID): Product
    fun findById(productId: UUID): Product
}