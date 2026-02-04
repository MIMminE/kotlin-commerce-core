package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.domain.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderRepository : OrderRepository {

    private val store = ConcurrentHashMap<UUID, Order>()

    override fun save(order: Order): Order {
        store[order.id] = order
        return order
    }

    override fun findById(id: UUID): Order? =
        store[id]

    override fun existsById(id: UUID): Boolean =
        store.containsKey(id)

    override fun findAllByUserId(userId: String, pageable: Pageable): Page<Order> {
        val all = store.values
            .asSequence()
            .filter { it.userId == userId }
            .toList()

        val total = all.size.toLong()
        val from = pageable.offset.toInt().coerceAtLeast(0)
        val to = (from + pageable.pageSize).coerceAtMost(all.size)
        val content = if (from >= all.size) emptyList() else all.subList(from, to)

        return PageImpl(content, pageable, total)
    }

    override fun findByUserIdAndIdempotencyKey(
        userId: String,
        idempotencyKey: UUID
    ): Order? {
        return store.values.firstOrNull {
            it.userId == userId && it.idempotencyKey == idempotencyKey
        }
    }

    fun clear() {
        store.clear()
    }
}