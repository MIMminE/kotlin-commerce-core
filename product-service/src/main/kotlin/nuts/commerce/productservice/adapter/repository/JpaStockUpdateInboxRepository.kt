package nuts.commerce.productservice.adapter.repository

import nuts.commerce.productservice.model.StockUpdateInboxRecord
import nuts.commerce.productservice.port.repository.StockUpdateInboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaStockUpdateInboxRepository(private val stockUpdateInboxJpa: StockUpdateInboxJpa) : StockUpdateInboxRepository {
    override fun save(stockUpdateInbox: StockUpdateInboxRecord): StockUpdateInboxRecord {
        return stockUpdateInboxJpa.saveAndFlush(stockUpdateInbox)
    }

    override fun findById(id: String): StockUpdateInboxRecord? {
        return stockUpdateInboxJpa.findById(UUID.fromString(id)).orElse(null)
    }
}

@Repository
interface StockUpdateInboxJpa : JpaRepository<StockUpdateInboxRecord, UUID>