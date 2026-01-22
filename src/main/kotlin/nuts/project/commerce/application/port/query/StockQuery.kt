package nuts.project.commerce.application.port.query

import nuts.project.commerce.domain.stock.Stock
import java.util.UUID

interface StockQuery {
    fun findByProductId(productId: UUID): Stock?
}