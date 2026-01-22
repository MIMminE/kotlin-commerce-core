package nuts.project.commerce.application.port.query

import nuts.project.commerce.domain.order.Order
import java.util.UUID

interface OrderQuery {
    fun findById(id: UUID): Order?
}