package nuts.commerce.inventoryservice.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "reservation_items",
    indexes = [
        Index(name = "idx_reservation_items_reservation_id", columnList = "reservation_id"),
        Index(name = "idx_reservation_items_inventory_id", columnList = "inventory_id")
    ]
)
class ReservationItem protected constructor(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: UUID,

    @Column(name = "inventory_id", nullable = false, updatable = false)
    val inventoryId: UUID,
    @Column(name = "qty", nullable = false, updatable = false)
    val qty: Long,

    @Column(name = "reservation_id", nullable = false, updatable = false)
    val reservationId: UUID

) : BaseEntity() {

    companion object {
        fun create(
            id: UUID = UUID.randomUUID(),
            inventoryId: UUID,
            qty: Long,
            reservationId: UUID,
        ): ReservationItem {
            if (qty <= 0) throw IllegalArgumentException("qty must be > 0")

            return ReservationItem(
                id = id,
                inventoryId = inventoryId,
                qty = qty,
                reservationId = reservationId
            )
        }
    }
}

