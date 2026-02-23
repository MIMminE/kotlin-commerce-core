package nuts.commerce.productservice.testutil

import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryProductEventInboxRepository : ProductEventInboxRepository {
    private val store = ConcurrentHashMap<UUID, ProductEventInbox>()

    override fun save(stockUpdateInbox: ProductEventInbox): ProductEventInbox {
        store[stockUpdateInbox.inboxId] = stockUpdateInbox
        return stockUpdateInbox
    }

    override fun findById(id: UUID): ProductEventInbox? {
        return store.values.find { it.inboxId == id }
    }

    fun clear() { store.clear() }

    fun all(): List<ProductEventInbox> = store.values.toList()
}
