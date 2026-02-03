package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.utils.FixtureUtils
import org.junit.jupiter.api.Assertions.*
import org.springframework.data.domain.PageRequest
import java.util.UUID
import kotlin.test.Test

class GetOrdersUseCaseTest {

    private val orderRepository = InMemoryOrderRepository()
    private val getUseCase = GetOrdersUseCase(orderRepository)

    @Test
    fun `get은 해당 userId의 주문만 반환한다`() {
        orderRepository.save(FixtureUtils.orderFixtureCreated(UUID.randomUUID(), "user-1"))
        orderRepository.save(FixtureUtils.orderFixtureCreated(UUID.randomUUID(), "user-2"))

        // when ( 페이저블 동작은  검증 하지 않음)
        val page = getUseCase.get("user-1", PageRequest.of(0, 50))

        // then
        assertEquals(2, page.content.size)
        assertEquals(setOf("user-1"), page.content.map { it.userId }.toSet())
    }
}