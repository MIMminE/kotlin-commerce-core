package nuts.commerce.orderservice.model.domain

import jakarta.persistence.*
import nuts.commerce.orderservice.model.BaseEntity
import nuts.commerce.orderservice.model.domain.Money
import java.util.UUID

@Entity
@Table(
    name = "order_items",
    indexes = [
        Index(name = "ix_order_items_order_id", columnList = "order_id"),
        Index(name = "ix_order_items_product_id", columnList = "product_id")
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
    lateinit var productId: String
        protected set

    @Column(name = "qty", nullable = false)
    var qty: Int = 0
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
            productId: String,
            orderId: UUID,
            qty: Int,
            unitPrice: Money,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): OrderItem {
            require(productId.isNotBlank()) { "productId is required" }
            require(qty > 0) { "qty must be > 0" }
            require(unitPrice.amount >= 0) { "unitPrice.amount must be >= 0" }
            require(unitPrice.currency.isNotBlank()) { "unitPrice.currency is required" }

            return OrderItem().apply {
                this.id = idGenerator()
                this.orderId = orderId
                this.productId = productId
                this.qty = qty
                this.unitPrice = unitPrice
            }
        }

    }
}
