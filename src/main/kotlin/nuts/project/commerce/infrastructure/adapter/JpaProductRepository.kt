package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.ProductRepository
import nuts.project.commerce.domain.product.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaProductRepository(private val productJpa: ProductJpa) : ProductRepository {
    override fun findByIds(ids: List<UUID>): List<Product> {
        TODO("Not yet implemented")
    }

    override fun save(product: Product): Product {
        TODO("Not yet implemented")
    }

    interface ProductJpa : JpaRepository<Product, UUID>
}

