package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Inventory
import java.util.UUID

interface InventoryRepository {
    fun save(inventory: Inventory): UUID
    fun getAllCurrentInventory(): List<InventoryInfo>
    fun reserveInventory(productId: UUID, quantity: Long): Boolean
    fun confirmReservedInventory(productId: UUID, quantity: Long): Boolean
    fun releaseReservedInventory(productId: UUID, quantity: Long): Boolean
}

data class InventoryInfo(
    val inventoryId: UUID,
    val productId: UUID,
    val productName: String,
    val availableQuantity: Long
)