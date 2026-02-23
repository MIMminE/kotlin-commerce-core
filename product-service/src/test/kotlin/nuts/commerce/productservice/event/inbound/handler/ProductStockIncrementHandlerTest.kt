package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.testutil.InMemoryStockCache
import nuts.commerce.productservice.testutil.InMemoryProductEventInboxRepository
import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import nuts.commerce.productservice.event.inbound.ProductStockIncrementPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import nuts.commerce.productservice.testutil.TestTransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Suppress("NonAsciiCharacters")
class ProductStockIncrementHandlerTest {
    private lateinit var stockCache: InMemoryStockCache
    private lateinit var inboxRepo: InMemoryProductEventInboxRepository
    private lateinit var txTemplate: TestTransactionTemplate
    private val objectMapper = ObjectMapper()
    private lateinit var handler: ProductStockIncrementHandler

    @BeforeEach
    fun setup() {
        stockCache = InMemoryStockCache()
        inboxRepo = InMemoryProductEventInboxRepository()
        txTemplate = TestTransactionTemplate()
        handler = ProductStockIncrementHandler(stockCache, inboxRepo, txTemplate, objectMapper)
    }

    @Test
    fun `성공 - INCREMENT_STOCK 이벤트 처리`() {
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val initialStock = 5L
        val incrementQty = 3L

        // 초기 재고 설정
        stockCache.saveStock(productId, initialStock)

        val payload = ProductStockIncrementPayload(UUID.randomUUID(), productId, incrementQty)
        val event = ProductInboundEvent(eventId, InboundEventType.INCREMENT_STOCK, payload)

        handler.handle(event)

        assertEquals(1, inboxRepo.all().size)
        val saved = inboxRepo.all().first()
        assertEquals(eventId, saved.idempotencyKey)

        // 재고가 incrementQty만큼 증가했는지 확인
        assertEquals(initialStock + incrementQty, stockCache.getStock(productId))
    }

    @Test
    fun `실패 - 잘못된 이벤트 타입이면 IllegalArgumentException`() {
        val eventId = UUID.randomUUID()
        val payload = ProductStockIncrementPayload(UUID.randomUUID(), UUID.randomUUID(), 1L)
        val event = ProductInboundEvent(eventId, InboundEventType.CREATED, payload)

        assertThrows(IllegalArgumentException::class.java) {
            handler.handle(event)
        }
    }

    @Test
    fun `실패 - 캐시에 재고가 없으면 IllegalStateException`() {
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val payload = ProductStockIncrementPayload(UUID.randomUUID(), productId, 1L)
        val event = ProductInboundEvent(eventId, InboundEventType.INCREMENT_STOCK, payload)

        assertThrows(IllegalStateException::class.java) {
            handler.handle(event)
        }
    }
}

