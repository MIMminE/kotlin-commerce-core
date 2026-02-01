package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.adapter.repository.InMemoryOrderRepository
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import kotlin.test.Test

class GetOrdersUseCaseTest {


    private val orderRepository = InMemoryOrderRepository()
    private val createUseCase = CreateOrderUseCase(orderRepository)
    private val getUseCase = GetOrdersUseCase(orderRepository)

    @Test
    fun `get은 해당 userId의 주문만 반환한다`() {
        repeat(2) {
            createUseCase.create(
                CreateOrderUseCase.Command(
                    userId = "user-1",
                    items = listOf(CreateOrderUseCase.Item("p-1", 1, 1000L, "KRW")),
                    totalAmount = 1000L,
                    currency = "KRW"
                )
            )
        }

        createUseCase.create(
            CreateOrderUseCase.Command(
                userId = "user-2",
                items = listOf(CreateOrderUseCase.Item("p-9", 1, 5000L, "KRW")),
                totalAmount = 5000L,
                currency = "KRW"
            )
        )

        // when ( 페이저블 동작은  검증 하지 않음)
        val page = getUseCase.get("user-1", PageRequest.of(0, 50))

        // then
        assertEquals(2, page.content.size)
        assertEquals(setOf("user-1"), page.content.map { it.userId }.toSet())
    }
}