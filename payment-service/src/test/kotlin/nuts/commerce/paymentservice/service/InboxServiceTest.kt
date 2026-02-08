package nuts.commerce.paymentservice.service

import nuts.commerce.paymentservice.application.port.repository.InMemoryInboxRepository
import nuts.commerce.paymentservice.model.InboxStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("NonAsciiCharacters")
class InboxServiceTest {

    private val repo = InMemoryInboxRepository()
    private val service = InboxService(repo)

    @BeforeEach
    fun setup() {
        repo.clear()
    }

    @Test
    fun `동일 이벤트가 중복 등록되지 않는다`() {
        val eventId = UUID.randomUUID()
        val payload = "{}"

        val first = service.registerIfNotProcessed(eventId, "PaymentRequested", payload, Instant.now())
        val second = service.registerIfNotProcessed(eventId, "PaymentRequested", payload, Instant.now())

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun `등록후 처리(markProcessed)하면 다시 등록되지 않는다`() {
        val eventId = UUID.randomUUID()
        val payload = "{}"

        assertTrue(service.registerIfNotProcessed(eventId, "PaymentRequested", payload, Instant.now()))
        assertTrue(service.markProcessed(eventId, Instant.now()))
        // 처리된 이벤트는 다시 등록 불가
        assertFalse(service.registerIfNotProcessed(eventId, "PaymentRequested", payload, Instant.now()))
    }
}

