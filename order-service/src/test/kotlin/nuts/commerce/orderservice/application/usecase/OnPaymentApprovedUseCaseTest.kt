package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOutboxRepository
import nuts.commerce.orderservice.application.port.repository.InMemorySageRepository
import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.exception.OrderException
import nuts.commerce.orderservice.model.OrderSaga
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

@Suppress("NonAsciiCharacters")
class OnPaymentApprovedUseCaseTest {

    private val orderRepository = InMemoryOrderRepository()
    private val orderOutboxRepo = InMemoryOutboxRepository()
    private val orderSagaRepo = InMemorySageRepository()
    private val useCase = OnPaymentApprovedUseCase(orderRepository, orderSagaRepo, orderOutboxRepo, ObjectMapper())

    @BeforeEach
    fun setup() {
        orderRepository.clear()
        orderOutboxRepo.clear()
        orderSagaRepo.clear()
    }

    @Test
    fun `결제 승인 시 아웃박스 생성되고 사가가 PAYMENT_COMPLETED로 전이된다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"), status = Order.OrderStatus.PAYING, idGenerator = { orderId })
        orderRepository.save(order)

        val saga = OrderSaga.create(orderId, OrderSaga.SagaStatus.PAYMENT_REQUESTED)
        orderSagaRepo.save(saga)

        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(
            UUID.randomUUID(),
            orderId,
            UUID.randomUUID(),
            "{}"
        )

        useCase.handle(event)

        val savedOrder = orderRepository.findById(orderId) ?: fail("order not found")
        assertEquals(Order.OrderStatus.PAID, savedOrder.status)

        val savedSaga = orderSagaRepo.findByOrderId(orderId) ?: fail("saga not found")
        assertEquals(OrderSaga.SagaStatus.PAYMENT_COMPLETED, savedSaga.status)

        val outboxes = orderOutboxRepo.findByAggregateId(orderId)
        assertEquals(1, outboxes.size)
        assertEquals("PAYMENT_COMPLETED", outboxes.single().eventType.name)
    }

    @Test
    fun `중복 이벤트는 무시된다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"),
            status = Order.OrderStatus.PAYING,
            idGenerator = { orderId })
        orderRepository.save(order)

        val saga = OrderSaga.create(
            orderId,
            OrderSaga.SagaStatus.PAYMENT_REQUESTED
        )
        orderSagaRepo.save(saga)

        val eventId = UUID.randomUUID()
        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(eventId, orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)
        useCase.handle(event)

        val outboxes = orderOutboxRepo.findByAggregateId(orderId)
        assertEquals(1, outboxes.size)
    }

    @Test
    fun `주문이 존재하지 않으면 아웃박스가 생성되지 않는다`() {
        val orderId = UUID.randomUUID()
        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)

        val outboxes = orderOutboxRepo.findByAggregateId(orderId)
        assertEquals(0, outboxes.size)
    }

    @Test
    fun `이미 결제된 주문이면 아웃박스가 생성되지 않는다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"),
            status = Order.OrderStatus.PAYING,
            idGenerator = { orderId })

        order.applyPaymentApproved()
        orderRepository.save(order)

        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "{}")

        useCase.handle(event)

        val outboxes = orderOutboxRepo.findByAggregateId(orderId)
        assertEquals(0, outboxes.size)
    }

    // 사가가 PAYMENT_REQUESTED가 아니면 예외 발생
    @Test
    fun `사가가 PAYMENT_REQUESTED 상태가 아니면 InvalidTransition 예외가 발생한다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val order = Order.create(
            userId = "u-1",
            idempotencyKey = UUID.randomUUID(),
            items = items,
            total = Money(1000L, "KRW"),
            status = Order.OrderStatus.PAYING,
            idGenerator = { orderId })
        orderRepository.save(order)

        // saga를 default 상태(CREATED)로 생성
        val saga = OrderSaga.create(orderId)
        orderSagaRepo.save(saga)

        val event = OnPaymentApprovedUseCase.PaymentApprovedEvent(UUID.randomUUID(), orderId, UUID.randomUUID(), "{}")

        assertFailsWith<OrderException.InvalidTransition> {
            useCase.handle(event)
        }
    }
}