
package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderSagaRepository
import nuts.commerce.orderservice.model.domain.Money
import nuts.commerce.orderservice.model.domain.Order
import nuts.commerce.orderservice.model.domain.OrderItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import tools.jackson.databind.ObjectMapper

@Suppress("NonAsciiCharacters")
class OnPaymentFailedUseCaseTest {

    private lateinit var orderRepository: InMemoryOrderRepository
    private lateinit var orderSagaRepository: InMemoryOrderSagaRepository
    private lateinit var orderOutboxRepository: InMemoryOrderOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var useCase: OnPaymentFailedUseCase

    @BeforeEach
    fun setup() {
        orderRepository = InMemoryOrderRepository()
        orderSagaRepository = InMemoryOrderSagaRepository()
        orderOutboxRepository = InMemoryOrderOutboxRepository()
        objectMapper = ObjectMapper()
        useCase = OnPaymentFailedUseCase(orderRepository, orderSagaRepository, orderOutboxRepository, objectMapper)

        orderRepository.clear()
        orderSagaRepository.clear()
        orderOutboxRepository.clear()
    }

    @Test
    fun `주문이 없으면 아무 동작도 하지 않는다`() {
        val event = OnPaymentFailedUseCase.PaymentFailedEvent(eventId = "e1", orderId = UUID.randomUUID(), reason = "r")
        // should not throw
        useCase.handle(event)
    }

    @Test
    fun `주문이 PAID이면 아무 동작도 하지 않는다`() {
        val id = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", id, 1, Money(1000L, "KRW")))
        val order = Order.create(userId = "u1", idempotencyKey = UUID.randomUUID(), items = items, total = Money(1000L, "KRW"), status = Order.OrderStatus.PAID, idGenerator = { id })
        orderRepository.save(order)

        val event = OnPaymentFailedUseCase.PaymentFailedEvent(eventId = "e2", orderId = id, reason = "r")
        useCase.handle(event)

        val after = orderRepository.findById(id)!!
        assertEquals(Order.OrderStatus.PAID, after.status)
    }

    @Test
    fun `주문이 PAYMENT_FAILED이면 아무 동작도 하지 않는다`() {
        val id = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", id, 1, Money(1000L, "KRW")))
        val order = Order.create(userId = "u1", idempotencyKey = UUID.randomUUID(), items = items, total = Money(1000L, "KRW"), status = Order.OrderStatus.PAYMENT_FAILED, idGenerator = { id })
        orderRepository.save(order)

        val event = OnPaymentFailedUseCase.PaymentFailedEvent(eventId = "e3", orderId = id, reason = "r")
        useCase.handle(event)

        val after = orderRepository.findById(id)!!
        assertEquals(Order.OrderStatus.PAYMENT_FAILED, after.status)
    }

    @Test
    fun `주문이 PAYING이면 PAYMENT_FAILED로 상태변경된다`() {
        val id = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", id, 1, Money(1000L, "KRW")))
        val order = Order.create(userId = "u1", idempotencyKey = UUID.randomUUID(), items = items, total = Money(1000L, "KRW"), status = Order.OrderStatus.PAYING, idGenerator = { id })
        orderRepository.save(order)

        val event = OnPaymentFailedUseCase.PaymentFailedEvent(eventId = "e4", orderId = id, reason = "r")
        useCase.handle(event)

        val after = orderRepository.findById(id)!!
        assertEquals(Order.OrderStatus.PAYMENT_FAILED, after.status)
    }

    @Test
    fun `CREATED 상태 등에서 applyPaymentFailed 호출시 InvalidTransition은 전파된다`() {
        val id = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", id, 1, Money(1000L, "KRW")))
        val order = Order.create(userId = "u1", idempotencyKey = UUID.randomUUID(), items = items, total = Money(1000L, "KRW"), status = Order.OrderStatus.CREATED, idGenerator = { id })
        orderRepository.save(order)

        val event = OnPaymentFailedUseCase.PaymentFailedEvent(eventId = "e5", orderId = id, reason = "r")
        assertFailsWith<Exception> {
            useCase.handle(event)
        }
    }
}
