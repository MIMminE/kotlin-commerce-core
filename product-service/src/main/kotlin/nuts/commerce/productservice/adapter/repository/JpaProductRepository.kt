package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.port.repository.ProductRepository
import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.model.ProductStatus
import nuts.commerce.productservice.exception.ProductException
import org.springframework.dao.DataAccessException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaProductRepository(private val productJpa: ProductJpa) : ProductRepository {
    override fun save(product: Product): Product {
        try {
            return productJpa.save(product)
        } catch (e: DataAccessException) {
            throw ProductException.InvalidCommand(e.message ?: "failed to register product")
        }
    }

    override fun getActiveProducts(): List<Product> {
        try {
            return productJpa.findProductsByStatusIs(ProductStatus.ACTIVE)
        } catch (e: DataAccessException) {
            throw ProductException.InvalidCommand(e.message ?: "failed to list active products")
        }
    }

    override fun getActiveProduct(productId: UUID): Product {
        try {
            return productJpa.findByProductIdAndStatus(
                productId,
                ProductStatus.ACTIVE
            ) ?: throw ProductException.InvalidCommand("Product not found: $productId")
        } catch (e: DataAccessException) {
            throw ProductException.InvalidCommand(e.message ?: "failed to get active product")
        }
    }

    override fun findById(productId: UUID): Product {
        try {
            return productJpa.findById(productId).orElse(null)
        } catch (e: DataAccessException) {
            throw ProductException.InvalidCommand(e.message ?: "failed to find product by id")
        }
    }
}

interface ProductJpa : JpaRepository<Product, UUID> {
    fun findProductsByStatusIs(status: ProductStatus): List<Product>
    fun findByProductIdAndStatus(
        productId: UUID,
        status: ProductStatus
    ): Product?
}