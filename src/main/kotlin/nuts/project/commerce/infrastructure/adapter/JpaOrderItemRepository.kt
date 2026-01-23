package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.OrderItemRepository
import nuts.project.commerce.domain.order.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaOrderItemRepository(private val orderItemJpa: OrderItemJpa) : OrderItemRepository {
    override fun findById(orderItemId: UUID): OrderItem? {
        TODO("Not yet implemented")
    }

    override fun save(orderItem: OrderItem): OrderItem {
        TODO("Not yet implemented")
    }

    interface OrderItemJpa : JpaRepository<OrderItem, UUID>
}

