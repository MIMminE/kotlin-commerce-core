package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.domain.order.Order
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderRepository : OrderRepository {

    private val store = ConcurrentHashMap<UUID, Order>()

    override fun findById(id: UUID): Order? =
        store[id]

    override fun save(order: Order): Order {
        // 전제: Order에 id: UUID가 존재
        store[order.id] = order
        return order
    }

    fun clear() = store.clear()
}