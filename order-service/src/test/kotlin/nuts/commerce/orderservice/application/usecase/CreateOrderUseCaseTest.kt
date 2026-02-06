package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderRepository
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderSagaRepository
import nuts.commerce.orderservice.application.port.rest.ProductPriceSnapshot
import nuts.commerce.orderservice.application.port.rest.ProductRestClient
import nuts.commerce.orderservice.model.infra.OutboxRecord
import org.junit.jupiter.api.BeforeEach
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate

class CreateOrderUseCaseTest {
    private val orderRepository = InMemoryOrderRepository()
    private val orderOutboxRepository = InMemoryOrderOutboxRepository()
    private val orderSagaRepository = InMemoryOrderSagaRepository()
    private val objectMapper = ObjectMapper()
    private val txManager: PlatformTransactionManager = object : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()
        override fun commit(status: TransactionStatus) {}
        override fun rollback(status: TransactionStatus) {}
    }

    private lateinit var productClient: TestProductClient
    private lateinit var useCase: CreateOrderUseCase

    @BeforeEach
    fun setUp() {
        orderRepository.clear()
        orderOutboxRepository.clear()
        orderSagaRepository.clear()
        productClient = TestProductClient()
        // TransactionTemplate을 생성해서 CreateOrderUseCase에 전달
        val txTemplate = TransactionTemplate(txManager)
        useCase = CreateOrderUseCase(orderRepository, orderOutboxRepository, orderSagaRepository, productClient, objectMapper, txTemplate)
    }

    // 간단한 테스트용 ProductRestClient 스텁
    class TestProductClient(var snapshots: Map<String, ProductPriceSnapshot> = emptyMap()) : ProductRestClient {
        override fun getPriceSnapshots(productIds: List<String>): List<ProductPriceSnapshot> {
            return productIds.mapNotNull { snapshots[it] }
        }
    }

    @Test
    fun `주문 생성 성공 - order 저장과 outbox 저장이 1번씩 발생하고 결과 orderId가 저장된 orderId와 같다`() {
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
    fun `outbox - eventType이 RESERVE_INVENTORY_REQUEST로 저장된다`() {
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
    fun `outbox - aggregateId가 생성된 주문 id와 동일하다`() {
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
    fun `outbox - payload JSON이 saved order 기준으로 올바르게 생성된다`() {
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
    fun `주문 아이템 매핑 - items의 productId, qty, unitPrice가 command와 일치한다`() {
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