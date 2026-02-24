package nuts.commerce.orderservice.testutil

import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.port.repository.OrderItemRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderItemRepository : OrderItemRepository {

    private val store: MutableMap<UUID, OrderItem> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        orderItems.forEach { item ->
            store[item.id] = item
        }
        return orderItems
    }

    fun findAll(): List<OrderItem> {
        return store.values.toList()
    }

    fun findById(id: UUID): OrderItem? {
        return store[id]
    }
}

