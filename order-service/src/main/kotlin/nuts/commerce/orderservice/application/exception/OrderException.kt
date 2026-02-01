package nuts.commerce.orderservice.application.exception

import nuts.commerce.orderservice.domain.OrderStatus
import java.util.UUID

sealed class OrderException(message: String) : RuntimeException(message) {

    class InvalidTransition(
        val orderId: UUID?,
        val from: OrderStatus,
        val to: OrderStatus
    ) : OrderException("invalid transition: $from -> $to (orderId=$orderId)")

    class InvalidCommand(message: String) : OrderException(message)
}