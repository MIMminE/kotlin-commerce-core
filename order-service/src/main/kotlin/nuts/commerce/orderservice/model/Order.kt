package nuts.commerce.orderservice.model

import jakarta.persistence.*
import nuts.commerce.orderservice.exception.OrderException
import java.util.*
import kotlin.collections.fold

@Entity
@Table(
    name = "orders",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_orders_user_id_idempotency_key",
            columnNames = ["user_id", "idempotency_key"]
        )
    ]
)
class Order protected constructor(
    @Id
    val orderId: UUID,

    @Column(nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Column(nullable = false, updatable = false, length = 64)
    val userId: String,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val items: MutableList<OrderItem> = mutableListOf(),

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "total_amount", nullable = false)),
        AttributeOverride(
            name = "currency",
            column = Column(name = "total_currency", nullable = false, length = 8)
        )
    )
    val totalPrice: Money,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: OrderStatus,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            orderId: UUID = UUID.randomUUID(),
            idempotencyKey: UUID,
            userId: String,
            items: List<OrderItem>,
            status: OrderStatus = OrderStatus.CREATED,
        ): Order {

            return Order(
                orderId = orderId,
                idempotencyKey = idempotencyKey,
                userId = userId,
                items = items.toMutableList(),
                totalPrice = totalPrice,
                status = status
            )
        }
    }
}

enum class OrderStatus { CREATED, PAYING, PAID, PAYMENT_FAILED, CANCELED }