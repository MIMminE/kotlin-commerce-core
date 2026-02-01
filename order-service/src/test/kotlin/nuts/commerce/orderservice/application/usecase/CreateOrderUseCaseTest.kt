package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.adapter.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.exception.OrderException
import nuts.commerce.orderservice.domain.OrderStatus
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class CreateOrderUseCaseTest {
    private val orderRepository = InMemoryOrderRepository()
    private val useCase = CreateOrderUseCase(orderRepository, )

    @Test
    fun `주문 생성 시 orderId가 반환되고 저장소에 존재한다`() {
        val cmd = CreateOrderUseCase.Command(
            userId = "user-1",
            items = listOf(
                CreateOrderUseCase.Item(
                    productId = "p-1",
                    qty = 2,
                    unitPriceAmount = 1000L,
                    unitPriceCurrency = "KRW"
                )
            ),
            totalAmount = 2000L,
            currency = "KRW"
        )

        val result = useCase.create(cmd)

        assertNotNull(result.orderId)
        assertTrue(orderRepository.existsById(result.orderId))

        val saved = orderRepository.findById(result.orderId)!!
        assertEquals("user-1", saved.userId)
        assertEquals(OrderStatus.CREATED, saved.status)
        assertEquals(1, saved.items.size)
    }

    @Test
    fun `생성된 모든 OrderItem은 Order의 id와 동일한 orderId를 가진다`() {
        val cmd = CreateOrderUseCase.Command(
            userId = "user-1",
            items = listOf(
                CreateOrderUseCase.Item("p-1", 1, 1000L, "KRW"),
                CreateOrderUseCase.Item("p-2", 3, 2000L, "KRW"),
            ),
            totalAmount = 7000L,
            currency = "KRW"
        )

        val result = useCase.create(cmd)

        val saved = orderRepository.findById(result.orderId)!!
        assertTrue(saved.items.isNotEmpty())
        assertTrue(saved.items.all { it.orderId == saved.id })
    }

    @Test
    fun `items가 비어있으면 주문 생성이 실패한다`() {
        val cmd = CreateOrderUseCase.Command(
            userId = "user-1",
            items = emptyList(),
            totalAmount = 0L,
            currency = "KRW"
        )

        assertThrows(OrderException.InvalidCommand::class.java) {
            useCase.create(cmd)
        }
    }

    @Test
    fun `주문 총액이 아이템 합계와 일치한다`() {
        val cmd = CreateOrderUseCase.Command(
            userId = "user-1",
            items = listOf(
                CreateOrderUseCase.Item("p-1", 2, 1000L, "KRW"), // 2000
                CreateOrderUseCase.Item("p-2", 3, 2000L, "KRW"), // 6000
            ),
            totalAmount = 8000L,
            currency = "KRW"
        )

        val result = useCase.create(cmd)
        val saved = orderRepository.findById(result.orderId)!!

        assertEquals(8000L, saved.total.amount)
        assertEquals("KRW", saved.total.currency)
    }

    @Test
    fun `여러 번 생성하면 orderId는 서로 다르다`() {
        val cmd = CreateOrderUseCase.Command(
            userId = "user-1",
            items = listOf(CreateOrderUseCase.Item("p-1", 1, 1000L, "KRW")),
            totalAmount = 1000L,
            currency = "KRW"
        )

        val r1 = useCase.create(cmd)
        val r2 = useCase.create(cmd)

        assertNotEquals(r1.orderId, r2.orderId)
    }
}