package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaProductEventInboxRepository(private val stockUpdateInboxJpa: StockUpdateInboxJpa) : ProductEventInboxRepository {
    override fun save(stockUpdateInbox: ProductEventInbox): ProductEventInbox {
        return stockUpdateInboxJpa.saveAndFlush(stockUpdateInbox)
    }

    override fun findById(id: String): ProductEventInbox? {
        return stockUpdateInboxJpa.findById(UUID.fromString(id)).orElse(null)
    }
}

@Repository
interface StockUpdateInboxJpa : JpaRepository<ProductEventInbox, UUID>