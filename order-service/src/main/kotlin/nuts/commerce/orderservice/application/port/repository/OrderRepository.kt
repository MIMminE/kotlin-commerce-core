package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.domain.Order
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID


interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
    fun existsById(id: UUID): Boolean
    fun findAllByUserId(userId: String, pageable: Pageable) : Page<Order>
}