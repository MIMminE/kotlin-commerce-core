package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.model.Money
import nuts.commerce.orderservice.model.Order
import nuts.commerce.orderservice.model.OrderItem
import nuts.commerce.orderservice.usecase.GetOrdersUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("NonAsciiCharacters")
class GetOrdersUseCaseTest {

    private val orderRepository = InMemoryOrderRepository()
    private val getUseCase = GetOrdersUseCase(orderRepository)

    @BeforeEach
    fun setup() {
        orderRepository.clear()
    }

    @Test
    fun `get은 해당 userId의 주문만 반환한다`() {
        val id1 = UUID.randomUUID()
        val items1 = listOf(
            OrderItem.create(productId = "p-1", orderId = id1, qty = 1, unitPrice = Money(1000L, "KRW"))
        )
        val order1 = Order.create(
            userId = "user-1",
            idempotencyKey = UUID.randomUUID(),
            items = items1,
            total = Money(1000L, "KRW"),
            idGenerator = { id1 })
        orderRepository.save(order1)

        val id2 = UUID.randomUUID()
        val items2 = listOf(
            OrderItem.create(productId = "p-2", orderId = id2, qty = 2, unitPrice = Money(2000L, "KRW"))
        )
        val order2 = Order.create(
            userId = "user-1",
            idempotencyKey = UUID.randomUUID(),
            items = items2,
            total = Money(4000L, "KRW"),
            idGenerator = { id2 })
        orderRepository.save(order2)

        val id3 = UUID.randomUUID()
        val items3 = listOf(
            OrderItem.create(productId = "p-3", orderId = id3, qty = 1, unitPrice = Money(1500L, "KRW"))
        )
        val order3 = Order.create(
            userId = "user-2",
            idempotencyKey = UUID.randomUUID(),
            items = items3,
            total = Money(1500L, "KRW"),
            idGenerator = { id3 })
        orderRepository.save(order3)

        // when (페이저블 동작은 검증하지 않음)
        val page = getUseCase.get("user-1", PageRequest.of(0, 50))

        // then
        assertEquals(2, page.content.size)
        assertTrue(page.content.all { it.userId == "user-1" })
    }

    @Test
    fun `user에 해당하는 주문이 없으면 빈 페이지를 반환한다`() {
        // given: repository 비어있음

        // when
        val page = getUseCase.get("no-user", PageRequest.of(0, 50))

        // then
        assertEquals(0, page.content.size)
    }
}
