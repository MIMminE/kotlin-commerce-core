package nuts.commerce.orderservice.port.repository

import nuts.commerce.orderservice.model.OrderItem
import java.util.UUID

interface OrderItemRepository {
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
}