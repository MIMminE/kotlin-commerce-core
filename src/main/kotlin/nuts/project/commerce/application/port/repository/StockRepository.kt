package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.stock.Stock
import java.util.UUID

interface StockRepository {

    fun save(stock: Stock): Stock
    fun findByProductId(productId: UUID): Stock?
//    fun findByProductIds(productIds: List<UUID>): List<Stock>
}