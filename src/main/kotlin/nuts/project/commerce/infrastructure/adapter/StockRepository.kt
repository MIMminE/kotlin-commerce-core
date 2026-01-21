package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.InventoryRepositoryPort
import nuts.project.commerce.domain.stock.Stock
import nuts.project.commerce.infrastructure.repository.JpaStockRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class StockRepository(private val jpaStockRepository: JpaStockRepository) : InventoryRepositoryPort {

    override fun save(stock: Stock): Stock {
        return jpaStockRepository.save(stock)
    }

    override fun findByProductId(productId: UUID): Stock? {
        return jpaStockRepository.findById(productId).orElse(null)
    }
}