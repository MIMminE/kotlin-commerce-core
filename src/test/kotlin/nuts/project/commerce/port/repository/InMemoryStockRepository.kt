package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.domain.core.stock.Stock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryStockRepository : StockRepository {

    private val storeByProductId = ConcurrentHashMap<UUID, Stock>()

    override fun save(stock: Stock): Stock {
        storeByProductId[stock.productId] = stock
        return stock
    }

    override fun findByProductId(productId: UUID): Stock? =
        storeByProductId[productId]

    fun clear() = storeByProductId.clear()
}