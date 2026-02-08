package nuts.commerce.orderservice.model

import jakarta.persistence.*
import nuts.commerce.orderservice.exception.OrderException
import java.util.*

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_user_id", columnList = "user_id"),
        Index(name = "idx_orders_status", columnList = "status"),
        Index(name = "idx_orders_idempotency_key", columnList = "idempotency_key"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_orders_user_id_idempotency_key",
            columnNames = ["user_id", "idempotency_key"]
        )
    ]
)
class Order protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @Column(name = "idempotency_key", nullable = false, length = 64, updatable = false)
    lateinit var idempotencyKey: UUID
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0L
        protected set

    @Column(name = "user_id", nullable = false, length = 64, updatable = false)
    lateinit var userId: String
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: OrderStatus = OrderStatus.CREATED
        protected set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var items: MutableList<OrderItem> = mutableListOf()
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "total_amount", nullable = false)),
        AttributeOverride(name = "currency", column = Column(name = "total_currency", nullable = false, length = 8))
    )
    var total: Money = Money(0L, "KRW")
        protected set

    companion object {
        fun create(
            userId: String,
            idempotencyKey: UUID,
            items: List<OrderItem>,
            total: Money,
            status: OrderStatus = OrderStatus.CREATED,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): Order {
            if (userId.isBlank()) throw OrderException.InvalidCommand("userId is required")
            if (items.isEmpty()) throw OrderException.InvalidCommand("order items required")

            return Order().apply {
                this.id = idGenerator()
                this.userId = userId
                this.idempotencyKey = idempotencyKey
                this.status = status
                this.items = items.toMutableList()
                this.total = total
            }
        }
    }

    fun markPaying() {
        if (status != OrderStatus.CREATED) {
            throw OrderException.InvalidTransition(idOrNull(), status, OrderStatus.PAYING)
        }
        status = OrderStatus.PAYING
    }

    fun applyPaymentApproved() {
        if (status != OrderStatus.PAYING) {
            throw OrderException.InvalidTransition(idOrNull(), status, OrderStatus.PAID)
        }
        status = OrderStatus.PAID
    }

    fun applyPaymentFailed() {
        if (status != OrderStatus.PAYING) {
            throw OrderException.InvalidTransition(idOrNull(), status, OrderStatus.PAYMENT_FAILED)
        }
        status = OrderStatus.PAYMENT_FAILED
    }

    private fun idOrNull(): UUID? = if (this::id.isInitialized) id else null

    enum class OrderStatus { CREATED, PAYING, PAID, PAYMENT_FAILED, CANCELED }
}
