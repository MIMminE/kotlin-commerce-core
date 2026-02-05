package nuts.commerce.inventoryservice.application.usecase

import nuts.commerce.inventoryservice.application.port.message.InventoryCachePublisher
import nuts.commerce.inventoryservice.application.port.repository.InventoryRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class MarkAvailableUseCase(
    private val inventoryRepository: InventoryRepository,
    private val publisher: InventoryCachePublisher
) {

    fun execute(inventoryId: UUID): Result {
        val inv = inventoryRepository.findById(inventoryId) ?: throw NoSuchElementException("Inventory not found: $inventoryId")
        inv.available()
        val saved = inventoryRepository.save(inv)
        publisher.publish(saved.inventoryId, saved.productId, saved.quantity)
        return Result(saved.inventoryId)
    }

    data class Result(val inventoryId: UUID)
}
