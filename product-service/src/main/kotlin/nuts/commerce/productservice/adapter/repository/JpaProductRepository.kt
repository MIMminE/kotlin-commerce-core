package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.Product
import nuts.commerce.productservice.port.repository.ProductInfo
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaProductRepository(private val productJpa: ProductJpa) : ProductRepository {
    override fun save(product: Product): UUID {
        return productJpa.save(product).productId
    }

    override fun getAllProductInfo(): List<ProductInfo> {
        return productJpa.findAllProductInfo()
    }

    override fun getProduct(productId: UUID): ProductInfo? {
        return productJpa.findProductInfoByProductId(productId)
    }

}

interface ProductJpa : JpaRepository<Product, UUID> {

    @Query(
        """
        SELECT new nuts.commerce.productservice.port.repository.ProductInfo(
            p.productId,
            p.productName,
            new nuts.commerce.productservice.model.Money(p.price.amount, p.price.currency)
        )
        FROM Product p
        WHERE p.productId = :productId
        """
    )
    fun findProductInfoByProductId(productId: UUID): ProductInfo?

    @Query(
        """
        SELECT new nuts.commerce.productservice.port.repository.ProductInfo(
            p.productId,
            p.productName,
            new nuts.commerce.productservice.model.Money(p.price.amount, p.price.currency)
        )
        FROM Product p
        """
    )
    fun findAllProductInfo(): List<ProductInfo>
}