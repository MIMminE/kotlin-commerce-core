package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Inventory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryInventoryRepository : InventoryRepository{
    private val store: MutableMap<UUID, Inventory> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(inventory: Inventory): Inventory {
        store[inventory.inventoryId] = inventory
        return inventory
    }

    override fun findById(inventoryId: UUID): Inventory? = store[inventoryId]
}