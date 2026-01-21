package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.OrderRepositoryPort
import nuts.project.commerce.domain.order.Order
import nuts.project.commerce.infrastructure.repository.JpaOrderRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class OrderRepository(
    private val jpaOrderRepository: JpaOrderRepository
) : OrderRepositoryPort {

    override fun save(order: Order): Order {
        return jpaOrderRepository.save(order)
    }

    override fun findById(id: UUID): Order? {
        return jpaOrderRepository.findById(id).orElse(null)
    }
}