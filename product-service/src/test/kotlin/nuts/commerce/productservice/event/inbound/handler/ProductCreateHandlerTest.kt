package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.testutil.InMemoryStockCache
import nuts.commerce.productservice.testutil.InMemoryProductEventInboxRepository
import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductCreatedPayload
import nuts.commerce.productservice.event.inbound.ProductInboundEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import nuts.commerce.productservice.testutil.TestTransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Suppress("NonAsciiCharacters")
class ProductCreateHandlerTest {

    private lateinit var stockCache: InMemoryStockCache
    private lateinit var inboxRepo: InMemoryProductEventInboxRepository
    private lateinit var txTemplate: TestTransactionTemplate
    private val objectMapper = ObjectMapper()
    private lateinit var handler: ProductCreateHandler

    @BeforeEach
    fun setup() {
        stockCache = InMemoryStockCache()
        inboxRepo = InMemoryProductEventInboxRepository()
        txTemplate = TestTransactionTemplate()
        handler = ProductCreateHandler(stockCache, inboxRepo, txTemplate, objectMapper)
    }

    @Test
    fun `성공 - CREATED 이벤트 처리`() {
        val eventId = UUID.randomUUID()
        val payload = ProductCreatedPayload(UUID.randomUUID(), 10L)
        val event = ProductInboundEvent(eventId, InboundEventType.CREATED, payload)

        handler.handle(event)

        // inbox 저장 확인
        assertEquals(1, inboxRepo.all().size)
        val saved = inboxRepo.all().first()
        assertEquals(eventId, saved.idempotencyKey)

        // 캐시 저장 확인
        assertEquals(payload.stock, stockCache.getStock(payload.productId))
    }

    @Test
    fun `실패 - 잘못된 이벤트 타입이면 IllegalArgumentException`() {
        val eventId = UUID.randomUUID()
        val payload = ProductCreatedPayload(UUID.randomUUID(), 5L)
        val event = ProductInboundEvent(eventId, InboundEventType.INCREMENT_STOCK, payload)

        assertThrows(IllegalArgumentException::class.java) {
            handler.handle(event)
        }
    }
}

