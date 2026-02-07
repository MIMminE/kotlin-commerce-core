package nuts.commerce.inventoryservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import nuts.commerce.inventoryservice.exception.InventoryException
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

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Long,

    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Long,

    @Column(nullable = false)
    var status: InventoryStatus,

    @Version
    var version: Long? = null
) : BaseEntity() {

    // compatibility: original 'quantity' returns available (unreserved) quantity
    val quantity: Long
        get() = availableQuantity

    fun increaseQuantity(amount: Long) {
        if (amount < 0) {
            throw InventoryException.InvalidCommand("Amount to increase must be non-negative")
        }
        availableQuantity += amount
    }

    fun decreaseQuantity(amount: Long) {
        if (amount < 0) {
            throw InventoryException.InvalidCommand("Amount to decrease must be non-negative")
        }
        if (availableQuantity < amount) {
            throw InventoryException.InsufficientInventory(
                inventoryId = inventoryId,
                requested = amount,
                available = availableQuantity
            )
        }
        availableQuantity -= amount
    }

    fun reserve(amount: Long) {
        if (amount <= 0) throw InventoryException.InvalidCommand("reserve amount must be > 0")
        if (availableQuantity < amount) {
            throw InventoryException.InsufficientInventory(inventoryId = inventoryId, requested = amount, available = availableQuantity)
        }
        availableQuantity -= amount
        reservedQuantity += amount
    }

    fun unreserve(amount: Long) {
        if (amount <= 0) throw InventoryException.InvalidCommand("unreserve amount must be > 0")
        if (reservedQuantity < amount) throw InventoryException.InvalidCommand("not enough reserved quantity to unreserve")
        reservedQuantity -= amount
        availableQuantity += amount
    }

    fun processReserved(amount: Long) {
        if (amount <= 0) throw InventoryException.InvalidCommand("process amount must be > 0")
        if (reservedQuantity < amount) throw InventoryException.InvalidCommand("not enough reserved quantity to process")
        reservedQuantity -= amount
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
            availableQuantity: Long,
            reservationQuantity: Long = 0L,
            status: InventoryStatus = InventoryStatus.AVAILABLE
        ): Inventory {
            return Inventory(
                inventoryId = inventoryId,
                productId = productId,
                availableQuantity = availableQuantity,
                reservedQuantity = reservationQuantity,
                status = status
            )
        }
    }
}

enum class InventoryStatus {
    AVAILABLE,
    UNAVAILABLE,
    DELETED
}