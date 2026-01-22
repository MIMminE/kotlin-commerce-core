package nuts.project.commerce.infrastructure.jpa

import nuts.project.commerce.domain.product.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductJpa : JpaRepository<Product, UUID> {
}