package nuts.commerce.orderservice.model.domain

import nuts.commerce.orderservice.model.exception.OrderException
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderTest {

    @Test
    fun `생성 - 필수 값 누락이면 InvalidCommand 발생`() {
        val items = listOf<OrderItem>()
        assertFailsWith<OrderException.InvalidCommand> {
            Order.create(userId = "", idempotencyKey = UUID.randomUUID(), items = items, total = Money(1000L, "KRW"))
        }
    }

    @Test
    fun `생성 - 정상 생성 시 필드들이 설정된다`() {
        val orderId = UUID.randomUUID()
        val items = listOf(OrderItem.create("p-1", orderId, 1, Money(1000L, "KRW")))
        val idempotency = UUID.randomUUID()

        val order = Order.create(
            userId = "u1",
            idempotencyKey = idempotency,
            items = items,
            total = Money(1000L, "KRW"),
            idGenerator = { orderId })

        assertEquals(orderId, order.id)
        assertEquals("u1", order.userId)
        assertEquals(idempotency, order.idempotencyKey)
        assertEquals(Order.OrderStatus.CREATED, order.status)
        assertEquals(1, order.items.size)
        assertEquals(1000L, order.total.amount)
    }

    @Test
    fun `markPaying 성공 - CREATED - PAYING`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        order.markPaying()
        assertEquals(Order.OrderStatus.PAYING, order.status)
    }

    @Test
    fun `markPaying 실패 - CREATED가 아니면 InvalidTransition`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        order.markPaying()
        assertFailsWith<OrderException.InvalidTransition> { order.markPaying() }
    }

    @Test
    fun `applyPaymentApproved 성공 - PAYING  PAID`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        order.markPaying()
        order.applyPaymentApproved()
        assertEquals(Order.OrderStatus.PAID, order.status)
    }

    @Test
    fun `applyPaymentApproved 실패 - PAYING이 아니면 InvalidTransition`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        assertFailsWith<OrderException.InvalidTransition> { order.applyPaymentApproved() }
    }

    @Test
    fun `applyPaymentFailed 성공 - PAYING - PAYMENT_FAILED`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        order.markPaying()
        order.applyPaymentFailed()
        assertEquals(Order.OrderStatus.PAYMENT_FAILED, order.status)
    }

    @Test
    fun `applyPaymentFailed 실패 - PAYING이 아니면 InvalidTransition`() {
        val order = Order.create(
            userId = "u1",
            idempotencyKey = UUID.randomUUID(),
            items = listOf(OrderItem.create("p-1", UUID.randomUUID(), 1, Money(1000L, "KRW"))),
            total = Money(1000L, "KRW")
        )
        assertFailsWith<OrderException.InvalidTransition> { order.applyPaymentFailed() }
    }
}

