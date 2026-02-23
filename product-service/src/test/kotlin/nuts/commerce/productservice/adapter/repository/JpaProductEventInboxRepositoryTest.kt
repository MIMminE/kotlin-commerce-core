package nuts.commerce.productservice.adapter.repository

import com.fasterxml.jackson.databind.ObjectMapper
import nuts.commerce.productservice.model.ProductEventInbox
import nuts.commerce.productservice.port.repository.ProductEventInboxRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaProductEventInboxRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaProductEventInboxRepositoryTest {


    @Autowired
    lateinit var repository: ProductEventInboxRepository

    @Autowired
    lateinit var inboxJpa: ProductInboxJpa

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun clear() {
        inboxJpa.deleteAll()
    }

    companion object {
        @ServiceConnection
        @Container
        val postgres = PostgreSQLContainer("postgres:15.3-alpine")
    }

    @Test
    fun `save 후 inboxId로 조회된다`() {
        val idempotencyKey = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(
            mapOf(
                "productId" to UUID.randomUUID(),
                "stock" to 10
            )
        )

        val saved = repository.save(
            ProductEventInbox.create(
                idempotencyKey = idempotencyKey,
                payload = payload
            )
        )

        val found = repository.findById(saved.inboxId)

        assertNotNull(found)
        assertEquals(saved.inboxId, found.inboxId)
        assertEquals(idempotencyKey, found.idempotencyKey)
        assertEquals(payload, found.payload)
    }

    @Test
    fun `존재하지 않는 inboxId는 null을 반환한다`() {
        val missing = repository.findById(UUID.randomUUID())
        assertNull(missing)
    }

    @Test
    fun `같은 idempotencyKey로 재시도하면 유니크 제약으로 실패한다`() {
        val idempotencyKey = UUID.randomUUID()

        repository.save(
            ProductEventInbox.create(
                idempotencyKey = idempotencyKey,
                payload = objectMapper.writeValueAsString(
                    mapOf(
                        "productId" to UUID.randomUUID(),
                        "stock" to 5
                    )
                )
            )
        )

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            repository.save(
                ProductEventInbox.create(
                    idempotencyKey = idempotencyKey,
                    payload = objectMapper.writeValueAsString(
                        mapOf(
                            "productId" to UUID.randomUUID(),
                            "stock" to 7
                        )
                    )
                )
            )
        }
    }
}