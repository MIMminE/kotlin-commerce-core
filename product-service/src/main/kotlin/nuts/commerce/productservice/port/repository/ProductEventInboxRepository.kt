package nuts.commerce.productservice.port.repository

import nuts.commerce.productservice.model.ProductEventInbox
import java.util.UUID

interface ProductEventInboxRepository {
    fun save(stockUpdateInbox: ProductEventInbox): ProductEventInbox
    fun findById(id: UUID): ProductEventInbox?
}