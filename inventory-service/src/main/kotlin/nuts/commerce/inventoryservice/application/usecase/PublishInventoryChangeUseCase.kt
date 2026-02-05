package nuts.commerce.inventoryservice.application.usecase

import nuts.commerce.inventoryservice.application.port.message.InventoryCachePublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PublishInventoryChangeUseCase(private val publisher: InventoryCachePublisher) {

    fun execute(inventoryId: UUID, productId: UUID, quantity: Long) {
        publisher.publish(inventoryId, productId, quantity)
    }
}
