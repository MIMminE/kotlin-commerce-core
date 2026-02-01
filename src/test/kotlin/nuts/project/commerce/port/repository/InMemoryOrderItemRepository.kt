package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.OrderItemRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class InMemoryOrderItemRepository : OrderItemRepository {

    private val store = ConcurrentHashMap<UUID, OrderItem>()

    override fun findById(orderItemId: UUID): OrderItem? =
        store[orderItemId]

    override fun save(orderItem: OrderItem): OrderItem {
        store[orderItem.id] = orderItem
        return orderItem
    }

    fun clear() = store.clear()
}
