package nuts.commerce.inventoryservice.exception

import nuts.commerce.inventoryservice.model.InventoryStatus
import java.util.UUID

sealed class InventoryException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    class InvalidTransition(
        val inventoryId: UUID?,
        val from: InventoryStatus,
        val to: InventoryStatus
    ) : InventoryException("invalid transition: $from -> $to (inventoryId=$inventoryId)")

    class InvalidCommand(message: String) : InventoryException(message)
    class InsufficientInventory(inventoryId: UUID, requested: Long, available: Long) :
        InventoryException("insufficient inventory: inventoryId=$inventoryId, requested=$requested, available=$available")

    class NotFound(inventoryId: UUID) : InventoryException("inventory not found: $inventoryId")
}