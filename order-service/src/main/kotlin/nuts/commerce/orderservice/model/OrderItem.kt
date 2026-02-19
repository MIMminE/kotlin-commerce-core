package nuts.commerce.orderservice.model

import jakarta.persistence.*
import nuts.commerce.orderservice.exception.OrderException
import java.util.UUID

@Entity
@Table(
    name = "order_items",
    indexes = [
        Index(name = "idx_order_items_order_id", columnList = "order_id"),
        Index(name = "idx_order_items_product_id", columnList = "product_id")
    ]
)
class OrderItem protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @Column(name = "order_id", nullable = false, updatable = false)
    lateinit var orderId: UUID
        protected set

    @Column(name = "product_id", nullable = false, length = 64)
    lateinit var productId: UUID
        protected set

    @Column(name = "qty", nullable = false)
    var qty: Long = 0
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "unit_price_amount", nullable = false)),
        AttributeOverride(
            name = "currency",
            column = Column(name = "unit_price_currency", nullable = false, length = 8)
        )
    )
    lateinit var unitPrice: Money
        protected set

    companion object {
        fun create(
            productId: UUID,
            qty: Long,
            unitPrice: Money,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): OrderItem {
            if (qty <= 0) throw OrderException.InvalidCommand("qty must be > 0")
            if (unitPrice.amount < 0) throw OrderException.InvalidCommand("unitPrice.amount must be >= 0")
            if (unitPrice.currency.isBlank()) throw OrderException.InvalidCommand("unitPrice.currency is required")

            return OrderItem().apply {
                this.id = idGenerator()
                this.productId = productId
                this.qty = qty
                this.unitPrice = unitPrice
            }
        }
    }
}
