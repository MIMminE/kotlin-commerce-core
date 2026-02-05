package nuts.commerce.inventoryservice.application.port.repository

import nuts.commerce.inventoryservice.model.domain.Inventory
import java.util.UUID

interface InventoryRepository {
    fun save(inventory: Inventory): Inventory
    fun findById(inventoryId: UUID): Inventory?
}
