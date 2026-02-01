package nuts.commerce.orderservice.application.repository

import nuts.commerce.orderservice.domain.core.OrderItem
import java.util.UUID

interface OrderItemRepository {
    fun findByOrderId(orderId: UUID): List<OrderItem>
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
}