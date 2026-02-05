package nuts.commerce.inventoryservice.model.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import nuts.commerce.inventoryservice.model.BaseEntity
import nuts.commerce.inventoryservice.model.exception.InventoryException
import java.util.UUID

@Entity
@Table(
    name = "inventories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id"])]
)
class Inventory protected constructor(

    @Id
    val inventoryId: UUID,

    @Column(name = "product_id", nullable = false, updatable = false)
    val productId: UUID,

    @Column(nullable = false)
    var quantity: Long,

    @Column(nullable = false)
    var status: InventoryStatus,

    @Version
    var version: Long? = null
) : BaseEntity() {

    fun increaseQuantity(amount: Long) {
        if (amount < 0) {
            throw InventoryException.InvalidCommand("Amount to increase must be non-negative")
        }
        quantity += amount
    }

    fun decreaseQuantity(amount: Long) {
        if (amount < 0) {
            throw InventoryException.InvalidCommand("Amount to decrease must be non-negative")
        }
        if (quantity < amount) {
            throw InventoryException.InsufficientInventory(
                inventoryId = inventoryId,
                requested = amount,
                available = quantity
            )
        }
        quantity -= amount
    }

    fun unavailable() {
        if (status == InventoryStatus.UNAVAILABLE) {
            throw InventoryException.InvalidTransition(
                inventoryId = inventoryId,
                from = status,
                to = InventoryStatus.UNAVAILABLE
            )
        }
        status = InventoryStatus.UNAVAILABLE
    }

    fun available() {
        if (status == InventoryStatus.AVAILABLE) {
            throw InventoryException.InvalidTransition(
                inventoryId = inventoryId,
                from = status,
                to = InventoryStatus.AVAILABLE
            )
        }
        status = InventoryStatus.AVAILABLE
    }

    fun delete() {
        if (status == InventoryStatus.DELETED) {
            throw InventoryException.InvalidTransition(
                inventoryId = inventoryId,
                from = status,
                to = InventoryStatus.DELETED
            )
        }
        status = InventoryStatus.DELETED
    }

    companion object {
        fun create(
            inventoryId: UUID = UUID.randomUUID(),
            productId: UUID,
            quantity: Long,
            status: InventoryStatus = InventoryStatus.AVAILABLE
        ): Inventory {
            return Inventory(
                inventoryId = inventoryId,
                productId = productId,
                quantity = quantity,
                status = status
            )
        }
    }

    enum class InventoryStatus {
        AVAILABLE,
        UNAVAILABLE,
        DELETED
    }
}