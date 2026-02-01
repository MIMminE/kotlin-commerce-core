package nuts.commerce.orderservice.infra.repository

import nuts.commerce.orderservice.application.repository.OrderItemRepository
import nuts.commerce.orderservice.domain.core.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaOrderItemRepository(private val orderItemJpa: OrderItemJpa) : OrderItemRepository {
    override fun findByOrderId(orderId: UUID): List<OrderItem> {
        return orderItemJpa.findByOrderId(orderId)
    }

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        return orderItemJpa.saveAll(orderItems)
    }
}

interface OrderItemJpa : JpaRepository<OrderItem, UUID> {
    fun findByOrderId(orderId: UUID): List<OrderItem>
}