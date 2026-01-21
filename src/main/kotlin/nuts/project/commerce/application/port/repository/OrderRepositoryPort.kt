package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.order.Order
import java.util.UUID

interface OrderRepositoryPort {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
}