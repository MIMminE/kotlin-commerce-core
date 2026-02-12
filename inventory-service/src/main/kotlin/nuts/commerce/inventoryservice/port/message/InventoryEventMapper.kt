package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxInfo

interface InventoryEventMapper {
    val eventType: EventType
    fun map(outboxInfo: OutboxInfo): InventoryEvent
}