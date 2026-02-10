package nuts.commerce.inventoryservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import java.util.UUID

@Entity
@Table(
    name = "inventories",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "idempotency_key"])]
)
class Inventory protected constructor(

    @Id
    val inventoryId: UUID,

    @Column(name = "product_id", nullable = false, updatable = false)
    val productId: UUID,

    @Column(nullable = false)
    val idempotencyKey: UUID,

    @Column(name = "available_quantity", nullable = false)
    var availableQuantity: Long,

    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Long,

    @Column(nullable = false)
    var status: InventoryStatus,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            inventoryId: UUID = UUID.randomUUID(),
            idempotencyKey: UUID,
            productId: UUID,
            availableQuantity: Long,
            reservationQuantity: Long = 0L,
            status: InventoryStatus = InventoryStatus.AVAILABLE
        ): Inventory {
            return Inventory(
                inventoryId = inventoryId,
                idempotencyKey = idempotencyKey,
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
    UNAVAILABLE
}