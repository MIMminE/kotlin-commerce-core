package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaInventoryRepository(private val inventoryJpa: InventoryJpa) : InventoryRepository {

    override fun save(inventory: Inventory): Inventory {
        return inventoryJpa.save(inventory)
    }

    override fun findById(inventoryId: UUID): Inventory {
        return inventoryJpa.findById(inventoryId).orElseThrow()
    }
}

interface InventoryJpa : JpaRepository<Inventory, UUID> {
}