package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.core.order.OrderItem
import java.util.UUID

interface OrderItemRepository {
    fun findById(orderItemId: UUID): OrderItem?
    fun save(orderItem: OrderItem): OrderItem
}