package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.core.Order
import java.util.UUID

interface OrderRepository {
    fun findById(id: UUID): Order?
    fun save(order: Order): Order
}
