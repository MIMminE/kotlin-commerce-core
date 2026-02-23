package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.testutil.InMemoryStockCache
import nuts.commerce.productservice.testutil.InMemoryProductEventInboxRepository
import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import nuts.commerce.productservice.event.inbound.ProductStockDecrementPayload
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import nuts.commerce.productservice.testutil.TestTransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Suppress("NonAsciiCharacters")
class ProductStockDecrementHandlerTest {
    private lateinit var stockCache: InMemoryStockCache
    private lateinit var inboxRepo: InMemoryProductEventInboxRepository
    private lateinit var txTemplate: TestTransactionTemplate
    private val objectMapper = ObjectMapper()
    private lateinit var handler: ProductStockDecrementHandler

    @BeforeEach
    fun setup() {
        stockCache = InMemoryStockCache()
        inboxRepo = InMemoryProductEventInboxRepository()
        txTemplate = TestTransactionTemplate()
        handler = ProductStockDecrementHandler(stockCache, inboxRepo, txTemplate, objectMapper)
    }

    @Test
    fun `성공 - DECREMENT_STOCK 이벤트 처리`() {
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val initialStock = 10L
        val decrementQty = 2L

        // 초기 재고 설정
        stockCache.saveStock(productId, initialStock)

        val payload = ProductStockDecrementPayload(UUID.randomUUID(), productId, decrementQty)
        val event = ProductInboundEvent(eventId, InboundEventType.DECREMENT_STOCK, payload)

        handler.handle(event)

        assertEquals(1, inboxRepo.all().size)
        val saved = inboxRepo.all().first()
        assertEquals(eventId, saved.idempotencyKey)

        // 재고가 decrementQty만큼 감소했는지 확인
        assertEquals(initialStock - decrementQty, stockCache.getStock(productId))
    }

    @Test
    fun `실패 - 잘못된 이벤트 타입이면 IllegalArgumentException`() {
        val eventId = UUID.randomUUID()
        val payload = ProductStockDecrementPayload(UUID.randomUUID(), UUID.randomUUID(), 1L)
        val event = ProductInboundEvent(eventId, InboundEventType.CREATED, payload)

        assertThrows(IllegalArgumentException::class.java) {
            handler.handle(event)
        }
    }

    @Test
    fun `실패 - 캐시에 재고가 없으면 IllegalStateException`() {
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val payload = ProductStockDecrementPayload(UUID.randomUUID(), productId, 1L)
        val event = ProductInboundEvent(eventId, InboundEventType.DECREMENT_STOCK, payload)

        assertThrows(IllegalStateException::class.java) {
            handler.handle(event)
        }
    }
}

