package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.product.Product
import java.util.UUID

interface ProductRepository {
    fun findByIds(ids: List<UUID>) : List<Product>
    fun save(product: Product) : Product
}