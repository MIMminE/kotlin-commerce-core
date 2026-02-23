package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaProductEventInboxRepository(private val productInboxJpa: ProductInboxJpa) :
    ProductEventInboxRepository {
    override fun save(stockUpdateInbox: ProductEventInbox): ProductEventInbox {
        return productInboxJpa.saveAndFlush(stockUpdateInbox)
    }

    override fun findById(id: UUID): ProductEventInbox? {
        return productInboxJpa.findById(id).orElse(null)
    }
}

@Repository
interface ProductInboxJpa : JpaRepository<ProductEventInbox, UUID>