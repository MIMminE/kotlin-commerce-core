package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.InventoryEventMapper
import org.springframework.stereotype.Component

@Component
class EventMapperRegistry(mappers: List<InventoryEventMapper>) {
    private val registry: Map<EventType, InventoryEventMapper> = run {
        val grouped = mappers.groupBy { it.eventType }
        val duplicates = grouped.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate InventoryEventMapper for eventType(s): $duplicates"
        }
        grouped.mapValues { (_, v) -> v.single() }
    }

    fun map(outboxInfo: OutboxInfo): InventoryEvent {
        val mapper = registry[outboxInfo.eventType]
            ?: error("No InventoryEventMapper for eventType=${outboxInfo.eventType}")
        return mapper.map(outboxInfo)
    }
}