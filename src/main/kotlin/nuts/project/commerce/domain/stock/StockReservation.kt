package nuts.project.commerce.domain.stock

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID

@Entity
@Table(name = "inventory_holds")
class StockReservation : BaseEntity(){

    @Id
    @Column(name = "reservation_id", nullable = false, columnDefinition = "uuid")
    val reservationId: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    lateinit var orderId: UUID
        protected set

    @Column(name = "status", nullable = false, length = 20)
    var status: StockReservationStatus = StockReservationStatus.ACTIVE
        protected set

    @Version
    var version: Long

}