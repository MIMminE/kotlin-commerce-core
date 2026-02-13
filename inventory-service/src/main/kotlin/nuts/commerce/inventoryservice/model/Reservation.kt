package nuts.commerce.inventoryservice.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["order_id", "idempotency_key"])]
)
class Reservation protected constructor(

    @Id
    val reservationId: UUID,

    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: UUID,

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReservationStatus,

    @OneToMany(mappedBy = "reservationId", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<ReservationItem>,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            reservationId: UUID = UUID.randomUUID(),
            orderId: UUID,
            idempotencyKey: UUID,
            status: ReservationStatus = ReservationStatus.CREATED,
            items: List<ReservationItem> = mutableListOf()
        ): Reservation {
            return Reservation(
                reservationId = reservationId,
                orderId = orderId,
                idempotencyKey = idempotencyKey,
                status = status,
                items = items.toMutableList()
            )
        }
    }

    fun addItems(newItems: List<ReservationItem>) {
        items.addAll(newItems)
    }

    fun confirm() {
        require(status == ReservationStatus.CREATED) { "invalid transition $status -> COMMITTED" }
        status = ReservationStatus.COMMITTED
    }

    fun release() {
        require(status == ReservationStatus.CREATED || status == ReservationStatus.COMMITTED) { "invalid transition $status -> RELEASED" }
        status = ReservationStatus.RELEASED
    }

    fun fail(){
        require(status == ReservationStatus.CREATED) { "invalid transition $status -> FAILED" }
        status = ReservationStatus.FAILED
    }
}

enum class ReservationStatus {
    CREATED,
    COMMITTED,
    RELEASED,
    FAILED
}