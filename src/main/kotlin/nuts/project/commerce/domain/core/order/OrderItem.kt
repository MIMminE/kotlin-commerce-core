package nuts.project.commerce.domain.core.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import java.util.UUID


@Entity
@Table(name = "order_item")
class OrderItem protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID()

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: Order
        protected set

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    lateinit var productId: UUID
        protected set

    @Column(name = "qty", nullable = false)
    var qty: Long = 1
        protected set

    @Column(name = "unit_price_snapshot", nullable = false)
    var unitPriceSnapshot: Long = 0
        protected set

    companion object {
        fun of(productId: UUID, qty: Long, unitPriceSnapshot: Long): OrderItem {
            require(qty > 0) { "qty must be > 0" }
            require(unitPriceSnapshot >= 0) { "unitPriceSnapshot must be >= 0" }
            return OrderItem().apply {
                this.productId = productId
                this.qty = qty
                this.unitPriceSnapshot = unitPriceSnapshot
            }
        }
    }

    internal fun attachTo(order: Order) {
        this.order = order
    }
}