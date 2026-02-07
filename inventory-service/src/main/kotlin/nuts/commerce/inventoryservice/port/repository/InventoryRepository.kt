package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.Inventory
import java.util.UUID

interface InventoryRepository {
    fun save(inventory: Inventory): Inventory
    fun findById(inventoryId: UUID): Inventory
}