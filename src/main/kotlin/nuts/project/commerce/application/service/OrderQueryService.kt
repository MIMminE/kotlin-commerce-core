package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.domain.order.Order
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderQueryService(private val orderRepository: OrderRepository) {
    fun findById(id: UUID): Order {
        return orderRepository.findById(id) ?: throw NoSuchElementException("Order with id $id not found")
    }
}