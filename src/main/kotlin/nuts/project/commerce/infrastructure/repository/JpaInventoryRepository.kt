package nuts.project.commerce.infrastructure.repository

import nuts.project.commerce.domain.inventory.Inventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaInventoryRepository : JpaRepository<Inventory, UUID> {
}