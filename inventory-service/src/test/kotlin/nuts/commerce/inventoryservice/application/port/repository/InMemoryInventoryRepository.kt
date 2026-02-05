package nuts.commerce.inventoryservice.application.port.repository

import nuts.commerce.inventoryservice.model.domain.Inventory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryInventoryRepository : InventoryRepository {

    private val store: MutableMap<UUID, Inventory> = ConcurrentHashMap()

    override fun save(inventory: Inventory): Inventory {
        store[inventory.inventoryId] = inventory
        return inventory
    }

    override fun findById(inventoryId: UUID): Inventory? = store[inventoryId]

    fun clear() = store.clear()
}
