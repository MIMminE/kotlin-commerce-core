package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.InventoryRepositoryPort
import nuts.project.commerce.domain.inventory.Inventory
import nuts.project.commerce.infrastructure.repository.JpaInventoryRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class InventoryRepository(private val jpaInventoryRepository: JpaInventoryRepository) : InventoryRepositoryPort {

    override fun save(inventory: Inventory): Inventory {
        return jpaInventoryRepository.save(inventory)
    }

    override fun findByProductId(productId: UUID): Inventory? {
        return jpaInventoryRepository.findById(productId).orElse(null)
    }
}