package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.OrderItemRepository
import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.domain.core.Order
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    fun save(order: Order): Order {

        order.items
            .forEach { orderItemRepository.save(it) }

        return orderRepository.save(order)
    }

    fun findById(id: UUID): Order {
        return orderRepository.findById(id) ?: throw NoSuchElementException("Order with id $id not found")
    }
}