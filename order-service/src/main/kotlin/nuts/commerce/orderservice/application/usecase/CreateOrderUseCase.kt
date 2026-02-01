package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.repository.OrderOutboxRepository
import nuts.commerce.orderservice.application.repository.OrderRepository
import nuts.commerce.orderservice.domain.Money
import nuts.commerce.orderservice.domain.core.Order
import nuts.commerce.orderservice.domain.core.OrderItem
import nuts.commerce.orderservice.domain.core.OrderOutboxEvent
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class CreateOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderOutboxRepository: OrderOutboxRepository,
    private val objectMapper: ObjectMapper
) {
    @Transactional
    fun create(command: Command): Result {
        val orderId = UUID.randomUUID()
        val orderItems: List<OrderItem> = command.items.map {
            OrderItem.create(
                productId = it.productId,
                orderId = orderId,
                qty = it.qty,
                unitPrice = Money(it.unitPriceAmount, it.unitPriceCurrency)
            )
        }

        val order = Order.create(
            userId = command.userId,
            items = orderItems,
            total = Money(command.totalAmount, command.currency),
            idGenerator = { orderId }
        )

        val saved = orderRepository.save(order)

        val outboxEvent = OrderOutboxEvent.create(
            aggregateId = UUID.randomUUID(),
            eventType = "OrderCreated",
            payload = objectMapper.writeValueAsString(
                OutboxEventPayload(
                    orderId = saved.id,
                    userId = saved.userId,
                    totalAmount = saved.total.amount,
                    currency = saved.total.currency,
                )
            )
        )

        orderOutboxRepository.save(outboxEvent)
        order.markPaying()

        return Result(saved.id)
    }
}

data class Command(
    val userId: String,
    val items: List<Item>,
    val totalAmount: Long,
    val currency: String,
)

data class Item(
    val productId: String,
    val qty: Int,
    val unitPriceAmount: Long,
    val unitPriceCurrency: String,
)

data class OutboxEventPayload(
    val orderId: UUID,
    val userId: String,
    val totalAmount: Long,
    val currency: String,
)

data class Result(val orderId: UUID)