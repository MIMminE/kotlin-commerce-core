package nuts.project.commerce.application.usecase

import nuts.project.commerce.application.exception.CouponNotFoundException
import nuts.project.commerce.application.exception.InvalidCouponException
import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.application.port.repository.IdempotencyRepository
import nuts.project.commerce.application.port.repository.OrderItemRepository
import nuts.project.commerce.application.port.repository.OrderRepository
import nuts.project.commerce.application.port.repository.ProductRepository
import nuts.project.commerce.application.port.repository.StockRepository
import nuts.project.commerce.application.port.repository.StockReservationRepository
import nuts.project.commerce.application.service.CouponService
import nuts.project.commerce.application.service.IdempotencyService
import nuts.project.commerce.application.service.OrderService
import nuts.project.commerce.application.service.ProductService
import nuts.project.commerce.application.service.StockService
import nuts.project.commerce.application.usecase.dto.PlaceOrderCommand
import nuts.project.commerce.application.usecase.dto.PlaceOrderResult
import nuts.project.commerce.domain.core.coupon.Coupon
import nuts.project.commerce.domain.core.coupon.CouponType
import nuts.project.commerce.domain.core.Product
import nuts.project.commerce.domain.core.stock.Stock
import nuts.project.commerce.port.repository.InMemoryCouponRepository
import nuts.project.commerce.port.repository.InMemoryIdempotencyRepository
import nuts.project.commerce.port.repository.InMemoryOrderItemRepository
import nuts.project.commerce.port.repository.InMemoryOrderRepository
import nuts.project.commerce.port.repository.InMemoryProductRepository
import nuts.project.commerce.port.repository.InMemoryStockRepository
import nuts.project.commerce.port.repository.InMemoryStockReservationRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceOrderUseCaseTest {

    private lateinit var couponRepository: CouponRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var stockRepository: StockRepository
    private lateinit var orderRepository: OrderRepository
    private lateinit var orderItemRepository: OrderItemRepository
    private lateinit var stockReservationRepository: StockReservationRepository
    private lateinit var idempotencyRepository: IdempotencyRepository

    private lateinit var useCase: PlaceOrderUseCase

    @BeforeEach
    fun setUp() {
        couponRepository = InMemoryCouponRepository()
        productRepository = InMemoryProductRepository()
        stockRepository = InMemoryStockRepository()
        orderRepository = InMemoryOrderRepository()
        orderItemRepository = InMemoryOrderItemRepository()
        stockReservationRepository = InMemoryStockReservationRepository()
        idempotencyRepository = InMemoryIdempotencyRepository()

        useCase = PlaceOrderUseCase(
            orderService = OrderService(orderRepository, orderItemRepository),
            stockService = StockService(stockRepository, stockReservationRepository),
            couponService = CouponService(couponRepository),
            productService = ProductService(productRepository),
            idempotencyService = IdempotencyService(idempotencyRepository)
        )
    }

    @Test
    fun `같은 멱등키로 place를 2번 호출해도 주문-재고예약이 중복 생성되지 않는다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val idemKey = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product 1",
                price = Money(10_000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )

        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(PlaceOrderCommand.Item(productId = productId, qty = 2)),
            couponId = null,
            commandIdempotencyKey = idemKey
        )

        // when: 같은 요청을 2번
        val first = useCase.place(command)
        val second = useCase.place(command)

        println(second)
    }


    @Test
    fun `상품이 존재하고 재고가 충분하면 주문이 생성되고 재고가 예약된다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product 1",
                price = Money(10000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )

        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(
                PlaceOrderCommand.Item(productId = productId, qty = 2)
            ),
            couponId = null,
            commandIdempotencyKey = UUID.randomUUID()
        )

        // when
        val result: PlaceOrderResult = useCase.place(command)

        // then
        assertNotNull(result.orderId)
        assertEquals(20_000, result.finalAmount)

        val updatedStock = stockRepository.findByProductId(productId)!!
        assertEquals(5, updatedStock.onHandQty)
        assertEquals(2, updatedStock.reservedQty)

        val reservations = stockReservationRepository.findByOrderId(result.orderId)
        assertTrue(reservations.isNotEmpty())
    }


    @Test
    fun `쿠폰이 유효하면 할인 적용된 금액으로 주문이 생성된다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val couponId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product 1",
                price = Money(10000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )

        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )
        couponRepository.save(
            Coupon.create(
                id = couponId,
                minOrderAmount = 0L,
                type = CouponType.FIXED_AMOUNT,
                value = 2000L
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(PlaceOrderCommand.Item(productId = productId, qty = 2)),
            couponId = couponId,
            commandIdempotencyKey = UUID.randomUUID()
        )

        // when
        val result = useCase.place(command)

        assertEquals(18_000, result.finalAmount)
    }

    @Test
    fun `쿠폰이 존재하지 않으면 예외가 발생한다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val missingCouponId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product 1",
                price = Money(10_000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )

        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(PlaceOrderCommand.Item(productId = productId, qty = 1)),
            couponId = missingCouponId,
            commandIdempotencyKey = UUID.randomUUID()
        )

        assertThrows(CouponNotFoundException::class.java) {
            useCase.place(command)
        }
    }

    @Test
    fun `쿠폰 최소 주문 금액을 만족하지 않으면 예외가 발생한다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val couponId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product",
                price = Money(10_000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )
        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )

        // 주문금액: 10,000 (qty=1) / 최소주문금액: 50,000 → 유효하지 않음
        couponRepository.save(
            Coupon.create(
                id = couponId,
                minOrderAmount = 50_000L,
                type = CouponType.FIXED_AMOUNT,
                value = 2_000L
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(PlaceOrderCommand.Item(productId = productId, qty = 1)),
            couponId = couponId,
            commandIdempotencyKey = UUID.randomUUID()
        )

        // when & then
        assertThrows(InvalidCouponException::class.java) {
            useCase.place(command)
        }
    }

    @Test
    fun `쿠폰이 기간 만료면 예외가 발생한다`() {
        // given
        val productId = UUID.randomUUID()
        val stockId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val couponId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = productId,
                name = "Test Product",
                price = Money(10_000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )
        stockRepository.save(
            Stock.create(
                id = stockId,
                productId = productId,
                initialQty = 5
            )
        )

        couponRepository.save(
            Coupon.create(
                id = couponId,
                minOrderAmount = 0L,
                type = CouponType.FIXED_AMOUNT,
                value = 2_000L,
                validFrom = Instant.now().minusSeconds(3600), // 1 hour ago
                validTo = Instant.now().minusSeconds(1800) // 30 minutes ago
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(PlaceOrderCommand.Item(productId = productId, qty = 1)),
            couponId = couponId,
            commandIdempotencyKey = UUID.randomUUID()
        )

        // when & then
        assertThrows(InvalidCouponException::class.java) {
            useCase.place(command)
        }
    }

    @Test
    fun `주문 생성 시 재고 예약을 거는 상품만 재고 예약을 생성한다`() {
        // given
        val userId = UUID.randomUUID()

        val reservingProductId = UUID.randomUUID()
        val nonReservingProductId = UUID.randomUUID()

        val reservingStockId = UUID.randomUUID()
        val nonReservingStockId = UUID.randomUUID()

        productRepository.save(
            Product.create(
                id = reservingProductId,
                name = "Reserving Product",
                price = Money(10_000L),
                stockHandlingPolicy = StockHandlingPolicy.RESERVE_THEN_DEDUCT
            )
        )

        productRepository.save(
            Product.create(
                id = nonReservingProductId,
                name = "Non-Reserving Product",
                price = Money(7_000L),
                stockHandlingPolicy = StockHandlingPolicy.DEDUCT_ON_PAYMENT
            )
        )

        stockRepository.save(
            Stock.create(
                id = reservingStockId,
                productId = reservingProductId,
                initialQty = 10
            )
        )
        stockRepository.save(
            Stock.create(
                id = nonReservingStockId,
                productId = nonReservingProductId,
                initialQty = 10
            )
        )

        val command = PlaceOrderCommand(
            userId = userId,
            items = listOf(
                PlaceOrderCommand.Item(productId = reservingProductId, qty = 2),
                PlaceOrderCommand.Item(productId = nonReservingProductId, qty = 3),
            ),
            couponId = null,
            commandIdempotencyKey = UUID.randomUUID()
        )

        // when
        val result = useCase.place(command)

        // then
        val reservations = stockReservationRepository.findByOrderId(result.orderId)

        // 예약을 거는 상품만 reservation이 생겨야 함
        assertEquals(1, reservations.size)
        assertEquals(reservingProductId, reservations.first().productId)
        assertEquals(2, reservations.first().quantity)

        // 예약 정책 상품은 reservedQty가 증가
        val reservingStock = stockRepository.findByProductId(reservingProductId)!!
        assertEquals(10, reservingStock.onHandQty) // 예약 단계에서는 수량이 감소하지 않음
        assertEquals(2, reservingStock.reservedQty)

        // 예약 비대상 상품은 reservedQty가 증가하지 않아야 함
        val nonReservingStock = stockRepository.findByProductId(nonReservingProductId)!!
        assertEquals(0, nonReservingStock.reservedQty)
    }
}