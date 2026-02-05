package nuts.commerce.productservice.application.port.repository

import nuts.commerce.productservice.model.domain.Product
import nuts.commerce.productservice.model.domain.ProductStatus
import nuts.commerce.productservice.model.exception.ProductException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryProductRepository : ProductRepository {

    private val store: MutableMap<UUID, Product> = ConcurrentHashMap()

    override fun save(product: Product): Product {
        try {
            store[product.productId] = product
            return product
        } catch (e: Throwable) {
            throw ProductException.InvalidCommand(e.message ?: "failed to register product")
        }
    }

    override fun getActiveProducts(): List<Product> = try {
        store.values.filter { it.status == ProductStatus.ACTIVE }
    } catch (e: Throwable) {
        throw ProductException.InvalidCommand(e.message ?: "failed to list active products")
    }

    override fun getActiveProduct(productId: UUID): Product {
        try {
            return store[productId]?.takeIf { it.status == ProductStatus.ACTIVE }
                ?: throw ProductException.InvalidCommand("Product not found or inactive: $productId")
        } catch (e: ProductException.InvalidCommand) {
            throw e
        } catch (e: Throwable) {
            throw ProductException.InvalidCommand(e.message ?: "failed to get active product")
        }
    }

    override fun findById(productId: UUID): Product {
        return store[productId] ?: throw ProductException.InvalidCommand("Product not found: $productId")
    }

    fun clear() = store.clear()
}
