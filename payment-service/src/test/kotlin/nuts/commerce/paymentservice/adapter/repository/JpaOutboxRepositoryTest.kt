package nuts.commerce.paymentservice.adapter.repository

import com.fasterxml.jackson.databind.ObjectMapper
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.OutboxStatus
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaOutboxRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaOutboxRepositoryTest {

    @Autowired
    lateinit var repository: OutboxRepository

    @Autowired
    lateinit var outboxJpa: OutboxJpa

    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun clear() = outboxJpa.deleteAll()

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }


    @Test
    fun `새로운 Outbox 레코드를 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload
        )

        // when
        val savedId = repository.save(record)

        // then
        assertNotNull(savedId)
        val saved = outboxJpa.findById(savedId).orElseThrow()
        assertEquals(orderId, saved.orderId)
        assertEquals(paymentId, saved.paymentId)
        assertEquals(idempotencyKey, saved.idempotencyKey)
        assertEquals(OutboxStatus.PENDING, saved.status)
    }

    @Test
    fun `동일한 payment_id와 idempotency_key로 저장하려고 하면 실패한다`() {
        // given
        val orderId1 = UUID.randomUUID()
        val orderId2 = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val payload1 = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val payload2 = objectMapper.writeValueAsString(mapOf("reason" to "duplicate"))

        val record1 = OutboxRecord.create(
            orderId = orderId1,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload1
        )

        val record2 = OutboxRecord.create(
            orderId = orderId2,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_FAILED,
            payload = payload2
        )

        // when
        repository.save(record1)

        // then - 유니크 제약 조건 위반
        assertThrows<DataIntegrityViolationException> {
            repository.save(record2)
        }
    }


    @Test
    fun `PENDING 상태의 레코드를 선점할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        // outboxJpa를 직접 사용하여 저장
        val savedRecord = outboxJpa.saveAndFlush(record)
        val outboxId = savedRecord.outboxId

        // when
        val lockedBy = "worker-1"
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = lockedBy)

        // then
        assertEquals(1, result.size)
        assertEquals(1, result.outboxInfo.size)
        val outboxInfo = result.outboxInfo.first()
        assertEquals(orderId, outboxInfo.orderId)
        assertEquals(paymentId, outboxInfo.paymentId)

        // verify - 상태가 PROCESSING으로 변경되었는지 확인
        val updated = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PROCESSING, updated.status)
        assertEquals(lockedBy, updated.lockedBy)
        assertNotNull(updated.lockedUntil)
    }

    @Test
    fun `nextAttemptAt 시간이 도래하지 않은 레코드는 선점할 수 없다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            nextAttemptAt = Instant.now().plusSeconds(60)
        )
        val savedRecord = outboxJpa.saveAndFlush(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker-1")

        // then
        assertEquals(0, result.size)
        assertEquals(0, result.outboxInfo.size)

        // verify - 상태가 PENDING 유지
        val unchanged = outboxJpa.findById(savedRecord.outboxId).orElseThrow()
        assertEquals(OutboxStatus.PENDING, unchanged.status)
    }

    @Test
    fun `락이 잠금 시간까지 유지되는 동안 선점할 수 없다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val now = Instant.now()
        val futureTime = now.plusSeconds(30)
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))

        val record = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker-1",
            lockedUntil = futureTime,
            nextAttemptAt = now.minusSeconds(10)
        )
        outboxJpa.saveAndFlush(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker-2")

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `배치 크기만큼만 선점할 수 있다`() {
        // given
        val now = Instant.now()
        repeat(5) { i ->
            val payload = objectMapper.writeValueAsString(mapOf("index" to i))
            val record = OutboxRecord.create(
                orderId = UUID.randomUUID(),
                paymentId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = payload,
                nextAttemptAt = now.minusSeconds(10)
            )
            outboxJpa.saveAndFlush(record)
        }

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 3, lockedBy = "worker-1")

        // then
        assertEquals(3, result.size)
        assertEquals(3, result.outboxInfo.size)
    }

    @Test
    fun `여러 워커가 동시에 선점할 수 있으며 중복되지 않는다`() {
        // given
        val now = Instant.now()
        repeat(5) { i ->
            val payload = objectMapper.writeValueAsString(mapOf("index" to i))
            val record = OutboxRecord.create(
                orderId = UUID.randomUUID(),
                paymentId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = payload,
                nextAttemptAt = now.minusSeconds(10)
            )
            outboxJpa.saveAndFlush(record)
        }

        // when
        val result1 = repository.claimAndLockBatchIds(batchSize = 3, lockedBy = "worker-1")
        val result2 = repository.claimAndLockBatchIds(batchSize = 3, lockedBy = "worker-2")

        // then
        assertEquals(3, result1.size)
        assertEquals(2, result2.size)
        assertEquals(5, result1.size + result2.size)

        // verify - 각 워커의 레코드가 다른지 확인
        val allRecords = outboxJpa.findAll()
        val worker1Records = allRecords.filter { it.lockedBy == "worker-1" }
        val worker2Records = allRecords.filter { it.lockedBy == "worker-2" }

        assertEquals(3, worker1Records.size)
        assertEquals(2, worker2Records.size)
    }


    @Test
    fun `PROCESSING 상태의 레코드를 PUBLISHED로 마킹한다`() {
        // given
        val now = Instant.now()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker-1",
            lockedUntil = now.plusSeconds(60),
            nextAttemptAt = now
        )
        val outboxId = repository.save(record)

        // when
        repository.markPublished(outboxId, "worker-1")

        // then
        val updated = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PUBLISHED, updated.status)
        assertNull(updated.lockedBy)
        assertNull(updated.lockedUntil)
    }

    @Test
    fun `다른 워커가 선점한 레코드는 마킹할 수 없다`() {
        // given
        val now = Instant.now()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker-1",
            lockedUntil = now.plusSeconds(60),
            nextAttemptAt = now
        )
        val outboxId = repository.save(record)

        // when & then
        assertThrows<IllegalStateException> {
            repository.markPublished(outboxId, "worker-2")
        }

        // verify - 상태 미변경
        val unchanged = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PROCESSING, unchanged.status)
    }

    @Test
    fun `PROCESSING 상태의 레코드를 FAILED로 마킹할 수 있다`() {
        // given
        val now = Instant.now()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker-1",
            lockedUntil = now.plusSeconds(60),
            nextAttemptAt = now
        )
        val outboxId = repository.save(record)

        // when
        repository.markFailed(outboxId, "worker-1")

        // then
        val updated = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.FAILED, updated.status)
        assertNull(updated.lockedBy)
        assertNull(updated.lockedUntil)
    }

    @Test
    fun `다른 워커가 잠금한 FAILED 마킹은 실패한다`() {
        // given
        val now = Instant.now()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker-1",
            lockedUntil = now.plusSeconds(60),
            nextAttemptAt = now
        )
        val outboxId = repository.save(record)

        // when & then
        assertThrows<IllegalStateException> {
            repository.markFailed(outboxId, "worker-2")
        }

        // verify - 상태 미변경
        val unchanged = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PROCESSING, unchanged.status)
    }


    @Test
    fun `Outbox 전체 라이프사이클 저장-선점-퍼블리싱`() {
        // given: 1단계 - Outbox 저장
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        val outboxId = repository.save(record)

        // verify stage 1
        var saved = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PENDING, saved.status)

        // when: 2단계 - 선점 및 락킹
        val lockedBy = "payment-publisher"
        val claimResult = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = lockedBy)

        // verify stage 2
        assertEquals(1, claimResult.size)
        var processing = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PROCESSING, processing.status)
        assertEquals(lockedBy, processing.lockedBy)
        assertNotNull(processing.lockedUntil)

        // when: 3단계 - 퍼블리싱 완료
        repository.markPublished(outboxId, lockedBy)

        // verify stage 3
        val published = outboxJpa.findById(outboxId).orElseThrow()
        assertEquals(OutboxStatus.PUBLISHED, published.status)
        assertNull(published.lockedBy)
        assertNull(published.lockedUntil)
    }

    @Test
    fun `멱등성 보장 선점 시나리오`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payload1 = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record1 = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload1,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )

        // when: 첫 번째 저장
        val outboxId1 = repository.save(record1)

        // then: 첫 번째 저장 성공
        assertEquals(1, outboxJpa.findAll().size)

        // when: 동일한 idempotency_key로 다시 저장 시도
        val payload2 = objectMapper.writeValueAsString(mapOf("confirmed" to true))
        val record2 = OutboxRecord.create(
            orderId = orderId,
            paymentId = paymentId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.PAYMENT_CONFIRM,
            payload = payload2
        )

        // then: 멱등성 보장으로 실패
        assertThrows<DataIntegrityViolationException> {
            repository.save(record2)
        }
    }

}