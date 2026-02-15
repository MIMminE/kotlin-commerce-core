package nuts.commerce.productservice.usecase

import nuts.commerce.productservice.port.cache.StockCachePort
import nuts.commerce.productservice.port.repository.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class StockUpdateUseCase(
    private val stockCachePort: StockCachePort
) {
    fun execute(command: UpdateStockCommand) {
        command.stockUpdateItems.forEach {
            stockCachePort.saveStock(it.productId, it.expectQuantity, it.updateQuantity)
        }
    }
}

data class UpdateStockCommand(
    val stockUpdateItems: List<StockUpdateItem>
) {
    data class StockUpdateItem(
        val productId: UUID,
        val expectQuantity: Long,
        val updateQuantity: Long
    )
}