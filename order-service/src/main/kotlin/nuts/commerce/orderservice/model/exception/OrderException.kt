package nuts.commerce.orderservice.model.exception

import nuts.commerce.orderservice.model.domain.Order
import java.util.UUID

sealed class OrderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {

    class InvalidTransition(
        val orderId: UUID?,
        val from: Order.OrderStatus,
        val to: Order.OrderStatus
    ) : OrderException("invalid transition: $from -> $to (orderId=$orderId)")

    class InvalidCommand(message: String) : OrderException(message)

    class MessagePublishFailed(
        message: String,
        cause: Throwable? = null,
        val eventId: UUID? = null,
        val aggregateId: UUID? = null,
        val eventType: String? = null,
    ) : OrderException(message, cause)
}