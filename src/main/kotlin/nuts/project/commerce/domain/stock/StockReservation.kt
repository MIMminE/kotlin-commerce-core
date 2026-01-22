package nuts.project.commerce.domain.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(name = "stock_reservation")
class StockReservation : BaseEntity() {

    @Id
    @Column(name = "reservation_id", nullable = false, columnDefinition = "uuid")
    val reservationId: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    lateinit var orderId: UUID
        protected set

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    lateinit var productId: UUID
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Long = 0
        protected set

    @Column(name = "status", nullable = false, length = 20)
    var status: StockReservationStatus = StockReservationStatus.ACTIVE
        protected set

    @Column(name = "reserved_until", nullable = true)
    var reservedUntil: Long? = null
        protected set

    companion object {
        fun create(orderId: UUID, productId: UUID, quantity: Long, reservedUntil: Long?): StockReservation {
            require(quantity > 0) { "Reservation quantity must be positive" }
            return StockReservation().apply {
                this.orderId = orderId
                this.productId = productId
                this.quantity = quantity
                this.reservedUntil = reservedUntil
            }
        }
    }

    fun confirm() {
        require(this.status == StockReservationStatus.ACTIVE) { "Only active reservations can be confirmed" }
        this.status = StockReservationStatus.CONFIRMED
    }

    fun release() {
        require(this.status == StockReservationStatus.ACTIVE) { "Only active reservations can be released" }
        this.status = StockReservationStatus.RELEASED

    }
}