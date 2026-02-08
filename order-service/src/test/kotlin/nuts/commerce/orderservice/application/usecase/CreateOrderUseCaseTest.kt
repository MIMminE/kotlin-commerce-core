package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderSagaRepository
import nuts.commerce.orderservice.application.port.rest.InMemoryProductRestClient
import nuts.commerce.orderservice.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.support.TestTransactionManager
import nuts.commerce.orderservice.usecase.Command
import nuts.commerce.orderservice.usecase.CreateOrderUseCase
import nuts.commerce.orderservice.usecase.Item
import nuts.commerce.orderservice.usecase.ReserveInventoryPayload
import org.junit.jupiter.api.BeforeEach
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@Suppress("NonAsciiCharacters")
class CreateOrderUseCaseTest {
    private val orderRepository = InMemoryOrderRepository()
    private val orderOutboxRepository = InMemoryOrderOutboxRepository()
    private val orderSagaRepository = InMemoryOrderSagaRepository()
    private val objectMapper = ObjectMapper()
    private val txManager: PlatformTransactionManager = TestTransactionManager()
    private val productClient = InMemoryProductRestClient()
    private lateinit var useCase: CreateOrderUseCase

    @BeforeEach
    fun setUp() {
        orderRepository.clear()
        orderOutboxRepository.clear()
        orderSagaRepository.clear()
        productClient
        val txTemplate = TransactionTemplate(txManager)
        useCase = CreateOrderUseCase(
            orderRepository,
            orderOutboxRepository,
            orderSagaRepository,
            productClient,
            objectMapper,
            txTemplate
        )
    }

    @Test
    fun `주문 생성 성공 - 주문과 아웃박스가 저장된다`() {
        // given
        val idemp = UUID.randomUUID()
        val command = Command(
            userId = "user-1",
            items = listOf(
                Item(
                    productId = "p-1",
                    qty = 2,
                    unitPriceAmount = 1000L,
                    unitPriceCurrency = "KRW",
                )
            ),
            totalAmount = 2000L,
            currency = "KRW",
            idempotencyKey = idemp
        )

        // product snapshot 준비 (스냅샷 가격을 명시)
        productClient.snapshots = mapOf("p-1" to ProductPriceSnapshot("p-1", 1000L, "KRW"))

        // when
        val result = useCase.create(command)
        val findOrder = orderRepository.findById(result.orderId)
        val orderOutBoxListByAggregateId = orderOutboxRepository.findByAggregateId(result.orderId)

        // then
        assertEquals(result.orderId, findOrder?.id)
        assertEquals(1, orderOutBoxListByAggregateId.size)
    }

    @Test
    fun `아웃박스 이벤트 타입은 RESERVE_INVENTORY_REQUEST 이다`() {
        // given
        val idemp = UUID.randomUUID()
        val command = Command(
            userId = "user-1",
            items = listOf(Item("p-1", 1, 1500L, "KRW")),
            totalAmount = 1500L,
            currency = "KRW",
            idempotencyKey = idemp
        )

        productClient.snapshots = mapOf("p-1" to ProductPriceSnapshot("p-1", 1500L, "KRW"))

        // when
        val result = useCase.create(command)

        // then
        val outboxes = orderOutboxRepository.findByAggregateId(result.orderId)
        assertEquals(1, outboxes.size)
        assertEquals("RESERVE_INVENTORY_REQUEST", outboxes.single().eventType.name)
    }

    @Test
    fun `아웃박스의 aggregateId는 생성된 주문 id와 같다`() {
        // given
        val idemp = UUID.randomUUID()
        val command = Command(
            userId = "user-1",
            items = listOf(Item("p-1", 1, 1000L, "KRW")),
            totalAmount = 1000L,
            currency = "KRW",
            idempotencyKey = idemp
        )

        productClient.snapshots = mapOf("p-1" to ProductPriceSnapshot("p-1", 1000L, "KRW"))

        // when
        val result = useCase.create(command)

        // then
        val outboxes = orderOutboxRepository.findByAggregateId(result.orderId)
        assertEquals(1, outboxes.size)
        assertEquals(result.orderId, outboxes.single().aggregateId)
    }

    @Test
    fun `아웃박스 payload는 저장된 주문을 반영한다`() {
        // given
        val idemp = UUID.randomUUID()
        val command = Command(
            userId = "user-777",
            items = listOf(
                Item("p-1", 3, 1000L, "KRW"),
                Item("p-2", 1, 5000L, "KRW"),
            ),
            totalAmount = 8000L,
            currency = "KRW",
            idempotencyKey = idemp
        )

        productClient.snapshots = mapOf(
            "p-1" to ProductPriceSnapshot("p-1", 1000L, "KRW"),
            "p-2" to ProductPriceSnapshot("p-2", 5000L, "KRW")
        )

        // when
        val result = useCase.create(command)

        // then
        val savedOrder = orderRepository.findById(result.orderId) ?: fail("저장된 Order가 없습니다.")
        val outbox: OutboxRecord = orderOutboxRepository.findByAggregateId(result.orderId).singleOrNull()
            ?: fail("aggregateId로 조회된 outbox가 1개여야 합니다.")

        val payload = objectMapper.readValue(outbox.payload, ReserveInventoryPayload::class.java)

        assertEquals(savedOrder.id, payload.orderId)
        assertEquals(savedOrder.items.size, payload.items.size)
    }

    @Test
    fun `주문 아이템 매핑은 command와 일치한다`() {
        // given
        val idemp = UUID.randomUUID()
        val command = Command(
            userId = "user-1",
            items = listOf(
                Item("p-1", 2, 1200L, "KRW"),
                Item("p-2", 1, 3300L, "KRW"),
            ),
            totalAmount = 5700L,
            currency = "KRW",
            idempotencyKey = idemp
        )

        productClient.snapshots = mapOf(
            "p-1" to ProductPriceSnapshot("p-1", 1200L, "KRW"),
            "p-2" to ProductPriceSnapshot("p-2", 3300L, "KRW")
        )

        // when
        val result = useCase.create(command)

        // then
        val savedOrder = orderRepository.findById(result.orderId) ?: fail("저장된 Order가 없습니다.")
        val savedItems = savedOrder.items
        assertEquals(command.items.size, savedItems.size)

        command.items.zip(savedItems).forEach { (cmdItem, savedItem) ->
            assertEquals(cmdItem.productId, savedItem.productId)
            assertEquals(cmdItem.qty, savedItem.qty)
            assertEquals(cmdItem.unitPriceAmount, savedItem.unitPrice.amount)
            assertEquals(cmdItem.unitPriceCurrency, savedItem.unitPrice.currency)
            assertEquals(savedOrder.id, savedItem.orderId)
        }
    }
}