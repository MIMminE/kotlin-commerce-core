package nuts.commerce.inventoryservice.testutil

import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.port.repository.InventoryInfo
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryInventoryRepository : InventoryRepository {
    private val store = ConcurrentHashMap<UUID, Inventory>()

    override fun save(inventory: Inventory): UUID {
        store[inventory.inventoryId] = inventory
        return inventory.inventoryId
    }

    override fun getAllCurrentInventory(): List<InventoryInfo> {
        return store.values.map {
            InventoryInfo(
                inventoryId = it.inventoryId,
                productId = it.productId,
                productName = it.productName,
                availableQuantity = it.availableQuantity
            )
        }
    }

    override fun reserveInventory(productId: UUID, quantity: Long): Boolean {
        val inventory = store.values.firstOrNull { it.productId == productId } ?: return false

        if (inventory.availableQuantity < quantity) {
            return false
        }

        inventory.availableQuantity -= quantity
        inventory.reservedQuantity += quantity
        store[inventory.inventoryId] = inventory
        return true
    }

    override fun confirmReservedInventory(productId: UUID, quantity: Long): Boolean {
        val inventory = store.values.firstOrNull { it.productId == productId } ?: return false

        if (inventory.reservedQuantity < quantity) {
            return false
        }

        inventory.reservedQuantity -= quantity
        store[inventory.inventoryId] = inventory
        return true
    }

    override fun releaseReservedInventory(productId: UUID, quantity: Long): Boolean {
        val inventory = store.values.firstOrNull { it.productId == productId } ?: return false

        if (inventory.reservedQuantity < quantity) {
            return false
        }

        inventory.availableQuantity += quantity
        inventory.reservedQuantity -= quantity
        store[inventory.inventoryId] = inventory
        return true
    }

    fun clear() {
        store.clear()
    }
}

