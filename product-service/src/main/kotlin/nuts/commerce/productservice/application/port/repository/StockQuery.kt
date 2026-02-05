package nuts.commerce.productservice.application.port.repository

import java.util.UUID

interface StockQuery {
    fun getStockQuantity(productId: UUID): Int
}