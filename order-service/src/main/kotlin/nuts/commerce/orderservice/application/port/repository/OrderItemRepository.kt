package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.domain.OrderItem
import java.util.UUID

interface OrderItemRepository {
    fun findByOrderId(orderId: UUID): List<OrderItem>
    fun saveAll(orderItems: List<OrderItem>): List<OrderItem>
}