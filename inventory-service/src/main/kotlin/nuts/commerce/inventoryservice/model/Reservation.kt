package nuts.commerce.inventoryservice.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "reservations",
    uniqueConstraints = [UniqueConstraint(columnNames = ["idempotency_key"])]
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

    ) : BaseEntity() {

    @OneToMany(mappedBy = "reservation", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<ReservationItem> = mutableListOf()
        protected set

    companion object {
        fun create(
            reservationId: UUID = UUID.randomUUID(),
            orderId: UUID,
            idempotencyKey: UUID,
            status: ReservationStatus = ReservationStatus.CREATED
        ): Reservation {
            return Reservation(
                reservationId = reservationId,
                orderId = orderId,
                idempotencyKey = idempotencyKey,
                status = status
            )
        }
    }

    fun addItems(items: List<ReservationItem>) {
        items.forEach {
            it.assignToReservation(this)
            this.items.add(it)
        }
    }

    fun startProcessing() {
        require(status == ReservationStatus.CREATED) { "invalid transition $status -> PROCESSING" }
        status = ReservationStatus.PROCESSING
    }

    fun commit() {
        require(status == ReservationStatus.PROCESSING) { "invalid transition $status -> COMMITTED" }
        status = ReservationStatus.COMMITTED
    }

    fun release() {
        require(status == ReservationStatus.PROCESSING || status == ReservationStatus.COMMITTED) { "invalid transition $status -> RELEASED" }
        status = ReservationStatus.RELEASED
    }
}

enum class ReservationStatus {
    CREATED,
    PROCESSING,
    COMMITTED,
    RELEASED
}