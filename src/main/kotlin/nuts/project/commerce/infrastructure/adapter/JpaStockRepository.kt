package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.domain.stock.Stock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaStockRepository(private val stockJpa: StockJpa) : StockRepository {
    override fun save(stock: Stock): Stock {
        return stockJpa.save(stock)
    }

    override fun findByProductId(productId: UUID): Stock? {
        return stockJpa.findById(productId).orElse(null)
    }

    interface StockJpa : JpaRepository<Stock, UUID>
}