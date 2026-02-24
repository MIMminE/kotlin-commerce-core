package nuts.commerce.orderservice.testutil

import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderStatus
import nuts.commerce.orderservice.port.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderRepository : OrderRepository {

    private val store: MutableMap<UUID, Order> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(order: Order): Order {
        order.validate()
        store[order.orderId] = order
        return order
    }

    override fun findById(id: UUID): Order? {
        return store[id]
    }

    override fun existsById(id: UUID): Boolean {
        return store.containsKey(id)
    }

    override fun findAllByUserId(userId: String, pageable: Pageable): Page<Order> {
        val filtered = store.values.filter { it.userId == userId }
        val start = pageable.offset.toInt()
        val end = minOf(start + pageable.pageSize, filtered.size)
        val content = if (start < filtered.size) filtered.subList(start, end) else emptyList()
        return PageImpl(content, pageable, filtered.size.toLong())
    }

    override fun findByUserIdAndIdempotencyKey(userId: String, idempotencyKey: UUID): Order? {
        return store.values.firstOrNull {
            it.userId == userId && it.idempotencyKey == idempotencyKey
        }
    }

    override fun updateStatus(orderId: UUID, expectStatus: OrderStatus, newStatus: OrderStatus) {
        val order = store[orderId] ?: return
        if (order.status == expectStatus) {
            order.status = newStatus
        }
    }
}

