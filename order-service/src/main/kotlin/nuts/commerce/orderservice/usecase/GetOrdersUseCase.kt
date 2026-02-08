package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.port.repository.OrderRepository
import nuts.commerce.orderservice.model.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetOrdersUseCase(
    private val orderRepository: OrderRepository
) {
    fun get(userId: String, pageable: Pageable): Page<Order> {
        return orderRepository.findAllByUserId(userId, pageable)
    }
}