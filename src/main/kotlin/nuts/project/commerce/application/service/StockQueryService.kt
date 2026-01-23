package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.domain.stock.Stock
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StockQueryService(private val stockRepository: StockRepository) {
    fun getStockByProductId(productId: UUID): Stock? {
        return stockRepository.findByProductId(productId)
    }
}