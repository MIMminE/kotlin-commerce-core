package nuts.commerce.productservice.port.repository

import nuts.commerce.productservice.model.StockUpdateInboxRecord

interface StockUpdateInboxRepository {
    fun save(stockUpdateInbox: StockUpdateInboxRecord): StockUpdateInboxRecord
    fun findById(id: String): StockUpdateInboxRecord?
}