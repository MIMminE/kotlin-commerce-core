package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.port.repository.OrderItemRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class
InMemoryOrderItemRepository : OrderItemRepository {

    private val byOrderId: MutableMap<UUID, MutableList<OrderItem>> = ConcurrentHashMap()

    override fun findByOrderId(orderId: UUID): List<OrderItem> =
        byOrderId[orderId]?.toList() ?: emptyList()

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        // orderId별로 append (필요하면 "교체" 정책으로 바꿔도 됨)
        orderItems.forEach { item ->
            val list = byOrderId.computeIfAbsent(item.orderId) { mutableListOf() }
            list.add(item)
        }
        return orderItems
    }

    fun clear() = byOrderId.clear()
}
