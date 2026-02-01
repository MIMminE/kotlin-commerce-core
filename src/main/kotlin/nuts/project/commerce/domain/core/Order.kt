package nuts.project.commerce.domain.core

import jakarta.persistence.*
import nuts.project.commerce.domain.common.OrderStatus

@Entity
@Table(name = "orders")
class Order(
    @Id
    var id: String = "",

    var clientId: String = "",

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.CREATED,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id")
    var items: MutableList<OrderItem> = mutableListOf()
) {
    fun markPaymentRequested() {
        if (status == OrderStatus.PAID) return
        status = OrderStatus.PAYMENT_REQUESTED
    }

    fun markPaid() {
        status = OrderStatus.PAID
    }

    fun markPaymentFailed() {
        if (status == OrderStatus.PAID) return
        status = OrderStatus.PAYMENT_FAILED
    }

    companion object {
        fun create(id: String, clientId: String, items: List<OrderItem>): Order =
            Order(id = id, clientId = clientId, status = OrderStatus.CREATED, items = items.toMutableList())
    }
}

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    var id: Long? = null,

    var productId: Long = 0,
    var quantity: Int = 0,
    var unitPrice: Int = 0
) {
    companion object {
        fun of(productId: Long, quantity: Int, unitPrice: Int): OrderItem =
            OrderItem(productId = productId, quantity = quantity, unitPrice = unitPrice)
    }
}
