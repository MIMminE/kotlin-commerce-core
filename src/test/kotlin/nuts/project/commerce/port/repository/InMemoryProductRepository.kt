package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.ProductRepository
import nuts.project.commerce.domain.core.product.Product
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryProductRepository : ProductRepository {

    private val store = ConcurrentHashMap<UUID, Product>()

    override fun findByIds(ids: List<UUID>): List<Product> {
        return ids.mapNotNull { store[it] }
    }

    override fun save(product: Product): Product {
        store[product.id] = product
        return product
    }

    fun clear() = store.clear()
}