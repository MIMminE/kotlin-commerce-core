package nuts.project.commerce.infrastructure.jpa

import nuts.project.commerce.domain.stock.Stock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StockJpa : JpaRepository<Stock, UUID> {
}