package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.OrderItemRepository
import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.domain.order.Order
import org.springframework.stereotype.Service

@Service
class OrderCommandService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository
) {
    fun save(order: Order): Order {

        order.items
            .forEach { orderItemRepository.save(it) }

        return orderRepository.save(order)
    }
}