package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.port.repository.OrderRepositoryPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class PlaceOrderUseCaseTest {

    private fun uuid() = UUID.randomUUID()
    private val p1 = uuid()
    private val p2 = uuid()
    private lateinit var orderRepositoryPort: OrderRepositoryPort
    private lateinit var useCase: PlaceOrderUseCase

    @BeforeEach
    fun setup() {
        orderRepositoryPort = FakeOrderRepositoryPort()
        val pricingPort = FakeProductQueryPort(
            mapOf(p1 to 1000L, p2 to 500L)
        )
        val couponPort = FakeCouponPolicyPort(discountAmount = 300L)
        useCase = PlaceOrderUseCase(orderRepositoryPort, pricingPort, couponPort)
    }

    @Test
    fun `쿠폰 없이 주문 생성 - 가격 스냅샷으로 금액 계산 후 저장된다`() {
        val userId = UUID.randomUUID()

        val result = useCase.place(
            PlaceOrderCommand(
                userId = userId,
                items = listOf(
                    PlaceOrderCommand.Item(productId = p1, qty = 2),
                    PlaceOrderCommand.Item(productId = p2, qty = 1),
                ),
                couponId = null
            )
        )

        assertEquals(2500L, result.originalAmount)
        assertEquals(0L, result.discountAmount)
        assertEquals(2500L, result.finalAmount)

        val saved = orderRepositoryPort.findById(result.orderId)!!
        assertEquals(userId, saved.userId)
        assertEquals(2, saved.items.size)
        assertEquals(2500L, saved.originalAmount)
    }

    @Test
    fun `쿠폰 포함 주문 생성 - 쿠폰 할인 적용 후 저장된다`() {
        val userId = uuid()
        val couponId = uuid()

        val result = useCase.place(
            PlaceOrderCommand(
                userId = userId,
                items = listOf(PlaceOrderCommand.Item(productId = p1, qty = 1)),
                couponId = couponId
            )
        )

        assertEquals(1000L, result.originalAmount)
        assertEquals(300L, result.discountAmount)
        assertEquals(700L, result.finalAmount)

        val saved = orderRepositoryPort.findById(result.orderId)!!
        assertEquals(couponId, saved.appliedCouponId)
        assertEquals(700L, saved.finalAmount)
    }

    @Test
    fun `주문 생성 - 아이템이 비어있으면 예외`() {
        val useCase = PlaceOrderUseCase(
            orderRepositoryPort = FakeOrderRepositoryPort(),
            productQueryPort = FakeProductQueryPort(emptyMap()),
            couponPolicyPort = FakeCouponPolicyPort(0L),
        )

        assertThrows<IllegalArgumentException> {
            useCase.place(
                PlaceOrderCommand(
                    userId = uuid(),
                    items = emptyList(),
                    couponId = null
                )
            )
        }
    }

    @Test
    fun `주문 생성 - 가격이 없는 상품이면 예외`() {
        val userId = uuid()
        val unknownProductId = uuid()

        val useCase = PlaceOrderUseCase(
            orderRepositoryPort = FakeOrderRepositoryPort(),
            productQueryPort = FakeProductQueryPort(priceByProductId = emptyMap()),
            couponPolicyPort = FakeCouponPolicyPort(0L),
        )

        assertThrows<IllegalArgumentException> {
            useCase.place(
                PlaceOrderCommand(
                    userId = userId,
                    items = listOf(PlaceOrderCommand.Item(unknownProductId, 1)),
                    couponId = null
                )
            )
        }
    }
}