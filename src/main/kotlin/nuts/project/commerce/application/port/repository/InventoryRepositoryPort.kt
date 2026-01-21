package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.inventory.Inventory
import java.util.UUID

interface InventoryRepositoryPort {
    fun save(inventory: Inventory): Inventory
    fun findByProductId(productId: UUID): Inventory?
}