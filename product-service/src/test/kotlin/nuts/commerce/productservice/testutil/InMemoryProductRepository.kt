package nuts.commerce.productservice.testutil

import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.port.repository.ProductInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InMemoryProductRepository : ProductRepository {
    private val store = ConcurrentHashMap<UUID, Product>()

    override fun save(product: Product): UUID {
        store[product.productId] = product
        return product.productId
    }

    override fun getAllProductInfo(): List<ProductInfo> = store.values.map { p ->
        ProductInfo(
            productId = p.productId,
            productName = p.productName,
            price = p.price
        )
    }

    override fun getProduct(productId: UUID): ProductInfo? {
        val p = store[productId] ?: return null
        return ProductInfo(productId = p.productId, productName = p.productName, price = p.price)
    }

    // 테스트 편의 메서드
    fun saveProduct(product: Product) { save(product) }
    fun clear() { store.clear() }
}

