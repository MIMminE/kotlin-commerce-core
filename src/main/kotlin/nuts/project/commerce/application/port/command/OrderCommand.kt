package nuts.project.commerce.application.port.command

import nuts.project.commerce.domain.order.Order

interface OrderCommand {
    fun save(order: Order): Order
}