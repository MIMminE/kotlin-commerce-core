package nuts.commerce.orderservice.utils

import nuts.commerce.orderservice.model.domain.Money
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderItem
import java.util.UUID

object FixtureUtils {
     fun orderFixtureCreated(id: UUID, userId: String): Order {
        val items = listOf(
            OrderItem.create(
                productId = "p-1",
                orderId = id,
                qty = 1,
                unitPrice = Money(1000L, "KRW")
            )
        )

        return Order.create(
            userId = userId,
            items = items,
            total = Money(1000L, "KRW"),
            idGenerator = { id }
        )
    }
}