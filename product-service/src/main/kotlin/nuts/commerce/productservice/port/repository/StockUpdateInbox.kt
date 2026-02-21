package nuts.commerce.productservice.port.repository

import nuts.commerce.productservice.model.ProductEventInbox

interface ProductEventInboxRepository {
    fun save(stockUpdateInbox: ProductEventInbox): ProductEventInbox
    fun findById(id: String): ProductEventInbox?
}