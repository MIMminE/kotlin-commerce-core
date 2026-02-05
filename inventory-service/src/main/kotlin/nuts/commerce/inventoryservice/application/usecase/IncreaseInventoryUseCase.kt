package nuts.commerce.inventoryservice.application.usecase

import nuts.commerce.inventoryservice.application.port.message.InventoryCachePublisher
import nuts.commerce.inventoryservice.application.port.repository.InventoryRepository
import nuts.commerce.inventoryservice.model.domain.Inventory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IncreaseInventoryUseCase(
    private val inventoryRepository: InventoryRepository,
    private val publisher: InventoryCachePublisher
) {

    fun execute(inventoryId: UUID, amount: Long): Result {
        val inv = inventoryRepository.findById(inventoryId) ?: throw NoSuchElementException("Inventory not found: $inventoryId")
        inv.increaseQuantity(amount)
        val saved = inventoryRepository.save(inv)
        publisher.publish(saved.inventoryId, saved.productId, saved.quantity)
        return Result(saved.inventoryId, saved.quantity)
    }

    data class Result(val inventoryId: UUID, val quantity: Long)
}
