package nuts.commerce.orderservice.application.usecase

import jakarta.transaction.Transactional
import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.OrderRepository
import nuts.commerce.orderservice.model.domain.Money
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderItem
import nuts.commerce.orderservice.model.integration.OrderOutboxRecord
import org.springframework.dao.DataIntegrityViolationException
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

        orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)
            ?.let { return Result(it.id) }

        return try {
            val saved = createAndPersistOrder(command)
            Result(saved.id)
        } catch (e: DataIntegrityViolationException) {
            Result(orderRepository.findByUserIdAndIdempotencyKey(command.userId, command.idempotencyKey)!!.id)
        }
    }

    private fun createAndPersistOrder(command: Command): Order {
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
            idGenerator = { orderId },
            idempotencyKey = command.idempotencyKey
        )
        val saved = orderRepository.save(order)

        val outboxEvent = OrderOutboxRecord.create(
            aggregateId = orderId,
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

        order.markPaying()
        orderOutboxRepository.save(outboxEvent)
        return saved
    }

    data class Command(
        val idempotencyKey: UUID,
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
}