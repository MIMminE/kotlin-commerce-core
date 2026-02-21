package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.outbound.ProductCreatedPayload
import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.port.message.ProductEventProducer
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.springframework.stereotype.Component

@Component
class PublishCurrentStockOnStartUp(
    private val inventoryRepository: InventoryRepository,
    private val productEventProducer: ProductEventProducer
) {
    fun publish() {
        val allCurrentInventory = inventoryRepository.getAllCurrentInventory()
        allCurrentInventory.forEach { inventory ->
            inventory.productId

            val event = ProductOutboundEvent(
                eventType = ProductEventType.CREATED,
                payload = ProductCreatedPayload(
                    productId = inventory.productId,
                    stock = inventory.availableQuantity
                )
            )
            productEventProducer.produce(event)
        }
    }
}