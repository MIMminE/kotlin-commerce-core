package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.model.ProductStatus
import nuts.commerce.productservice.port.repository.ProductInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaProductRepository(private val productJpa: ProductJpa) : ProductRepository {
    override fun save(product: Product): UUID {
        TODO("Not yet implemented")
    }

    override fun getActiveProducts(): List<ProductInfo> {
        TODO("Not yet implemented")
    }

    override fun getProduct(productId: UUID): Product {
        TODO("Not yet implemented")
    }
}

interface ProductJpa : JpaRepository<Product, UUID> {
    fun findProductsByStatusIs(status: ProductStatus): List<Product>
    fun findByProductIdAndStatus(
        productId: UUID,
        status: ProductStatus
    ): Product?
}