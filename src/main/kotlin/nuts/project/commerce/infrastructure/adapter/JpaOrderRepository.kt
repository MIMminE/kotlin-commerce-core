package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.domain.order.Order
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaOrderRepository(private val orderJpa: OrderJpa) : OrderRepository {
    override fun findById(id: UUID): Order? {
        TODO("Not yet implemented")
    }

    override fun save(order: Order): Order {
        TODO("Not yet implemented")
    }

    interface OrderJpa : JpaRepository<Order, UUID>
}