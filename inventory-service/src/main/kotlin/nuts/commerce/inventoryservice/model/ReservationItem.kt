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
class ReservationItem protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, updatable = false)
    lateinit var reservation: Reservation
        protected set

    @Column(name = "inventory_id", nullable = false, updatable = false)
    lateinit var inventoryId: UUID
        protected set

    @Column(name = "qty", nullable = false)
    var qty: Long = 0
        protected set

    companion object {
        fun create(
            reservation: Reservation,
            inventoryId: UUID,
            qty: Long,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): ReservationItem {
            if (qty <= 0) throw IllegalArgumentException("qty must be > 0")

            return ReservationItem().apply {
                this.id = idGenerator()
                this.reservation = reservation
                this.inventoryId = inventoryId
                this.qty = qty
            }
        }
    }

    fun assignToReservation(reservation: Reservation) {
        this.reservation = reservation
    }
}