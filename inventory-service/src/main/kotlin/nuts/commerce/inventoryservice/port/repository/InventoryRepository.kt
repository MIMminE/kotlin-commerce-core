package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Inventory
import java.util.UUID

interface InventoryRepository {
    fun save(inventory: Inventory): UUID
    fun findAllByProductIdIn(productIds: List<UUID>): List<InventoryInfo>
    fun findById(inventoryId: UUID): InventoryInfo?
    fun reserveInventory(productId: UUID, quantity: Long): Boolean
    fun commitReservedInventory(inventoryId: UUID, quantity: Long): Boolean
    fun releaseReservedInventory(inventoryId: UUID, quantity: Long): Boolean
}

data class InventoryInfo(val inventoryId: UUID, val productId: UUID, val availableQuantity: Long)