package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.port.repository.InventoryInfo
import nuts.commerce.inventoryservice.port.repository.InventoryRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaInventoryRepository(private val inventoryJpa: InventoryJpa) : InventoryRepository {

    override fun save(inventory: Inventory): UUID {
        return inventoryJpa.saveAndFlush(inventory).inventoryId
    }

    override fun findAllByProductIdIn(productIds: List<UUID>): List<InventoryInfo> {
        return inventoryJpa.findAllByProductIdIn(productIds)
    }

    override fun findById(inventoryId: UUID): InventoryInfo? {
        return inventoryJpa.findInventoryInfoById(inventoryId)
    }

    override fun reserveInventory(productId: UUID, quantity: Long): Boolean {
        return inventoryJpa.reserveInventory(productId, quantity) == 1
    }

    override fun commitReservedInventory(inventoryId: UUID, quantity: Long): Boolean {
        return inventoryJpa.commitReservedInventory(inventoryId, quantity) == 1
    }

    override fun releaseReservedInventory(inventoryId: UUID, quantity: Long): Boolean {
        return inventoryJpa.releaseReservedInventory(inventoryId, quantity) == 1
    }
}

interface InventoryJpa : JpaRepository<Inventory, UUID> {

    @Query(
        """
            select new nuts.commerce.inventoryservice.port.repository.InventoryInfo(
                i.inventoryId as inventoryId,
                i.productId as productId,
                i.availableQuantity as availableQuantity)
            from Inventory i 
            where i.productId in :productIds
        """
    )
    fun findAllByProductIdIn(@Param("productIds") productIds: List<UUID>): List<InventoryInfo>

    @Query(
        """
            select new nuts.commerce.inventoryservice.port.repository.InventoryInfo(
                i.inventoryId as inventoryId,
                i.productId as productId,
                i.availableQuantity as availableQuantity)
            from Inventory i 
            where i.inventoryId = :inventoryId
        """
    )
    fun findInventoryInfoById(@Param("inventoryId") inventoryId: UUID): InventoryInfo?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Inventory i
           set i.availableQuantity = i.availableQuantity - :quantity,
               i.reservedQuantity = i.reservedQuantity + :quantity
         where i.productId = :productId
           and i.availableQuantity >= :quantity
        """
    )
    fun reserveInventory(
        @Param("productId") productId: UUID,
        @Param("quantity") quantity: Long
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            update Inventory i
               set i.reservedQuantity = i.reservedQuantity - :quantity
             where i.inventoryId = :inventoryId
               and i.reservedQuantity >= :quantity
        """
    )
    fun commitReservedInventory(
        @Param("inventoryId") inventoryId: UUID,
        @Param("quantity") quantity: Long
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update Inventory i
           set i.reservedQuantity = i.reservedQuantity - :quantity,
               i.availableQuantity = i.availableQuantity + :quantity
         where i.inventoryId = :inventoryId
           and i.reservedQuantity >= :quantity
    """
    )
    fun releaseReservedInventory(inventoryId: UUID, quantity: Long): Int
}