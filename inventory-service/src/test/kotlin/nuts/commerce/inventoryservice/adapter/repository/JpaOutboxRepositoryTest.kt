package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
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
    fun clear() {
        outboxJpa.deleteAll()
    }

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `새로운 OutboxRecord를 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = 10L)
            )
        )
        val payload = objectMapper.writeValueAsString(payloadObject)

        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            reservationId = reservationId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = payload
        )

        // when
        val outboxId = repository.save(outboxRecord)

        // then
        assertNotNull(outboxId)
        val saved = outboxJpa.findById(outboxId).orElse(null)
        assertNotNull(saved)
        assertEquals(orderId, saved.orderId)
        assertEquals(reservationId, saved.reservationId)
        assertEquals(OutboxStatus.PENDING, saved.status)
    }

    @Test
    fun `PENDING 상태의 OutboxRecord를 claim하고 lock할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = 5L)
            )
        )
        val payload = objectMapper.writeValueAsString(payloadObject)

        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            reservationId = reservationId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = payload
        )
        repository.save(outboxRecord)

        // when
        val lockedBy = "worker-1"
        val result = repository.claimAndLockBatchIds(10, lockedBy)

        // then
        assertEquals(1, result.size)
        assertEquals(1, result.outboxInfo.size)
        assertEquals(orderId, result.outboxInfo[0].orderId)
        assertEquals(reservationId, result.outboxInfo[0].reservationId)
    }

    @Test
    fun `여러 개의 PENDING OutboxRecord를 batch로 claim할 수 있다`() {
        // given
        val count = 5
        (1..count).forEach { i ->
            val payloadObject = ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), (i * 10).toLong())
                )
            )
            val outboxRecord = OutboxRecord.create(
                orderId = UUID.randomUUID(),
                reservationId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payloadObject)
            )
            repository.save(outboxRecord)
        }

        // when
        val lockedBy = "worker-1"
        val result = repository.claimAndLockBatchIds(10, lockedBy)

        // then
        assertEquals(count, result.size)
        assertEquals(count, result.outboxInfo.size)
    }

    @Test
    fun `batchSize보다 적은 PENDING 레코드만 claim된다`() {
        // given
        val count = 3
        (1..count).forEach { i ->
            val payloadObject = ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), (i * 5).toLong())
                )
            )
            val outboxRecord = OutboxRecord.create(
                orderId = UUID.randomUUID(),
                reservationId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payloadObject)
            )
            repository.save(outboxRecord)
        }

        // when
        val lockedBy = "worker-1"
        val result = repository.claimAndLockBatchIds(10, lockedBy)

        // then
        assertEquals(count, result.size)
    }

    @Test
    fun `PENDING이 없으면 빈 결과를 반환한다`() {
        // given - 아무것도 저장하지 않음

        // when
        val lockedBy = "worker-1"
        val result = repository.claimAndLockBatchIds(10, lockedBy)

        // then
        assertEquals(0, result.size)
        assertTrue(result.outboxInfo.isEmpty())
    }

    @Test
    fun `OutboxRecord를 PUBLISHED로 mark할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
            )
        )
        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            reservationId = reservationId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObject)
        )
        repository.save(outboxRecord)

        val lockedBy = "worker-1"
        val claimResult = repository.claimAndLockBatchIds(1, lockedBy)
        val outboxId = claimResult.outboxInfo[0].outboxId

        // when
        repository.markPublished(outboxId, lockedBy)

        // then
        val updated = outboxJpa.findById(outboxId).orElse(null)
        assertNotNull(updated)
        assertEquals(OutboxStatus.PUBLISHED, updated.status)
    }

    @Test
    fun `OutboxRecord를 FAILED로 mark할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 20L)
            )
        )
        val outboxRecord = OutboxRecord.create(
            orderId = orderId,
            reservationId = reservationId,
            idempotencyKey = idempotencyKey,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObject)
        )
        repository.save(outboxRecord)

        val lockedBy = "worker-1"
        val claimResult = repository.claimAndLockBatchIds(1, lockedBy)
        val outboxId = claimResult.outboxInfo[0].outboxId

        // when
        repository.markFailed(outboxId, lockedBy)

        // then
        val updated = outboxJpa.findById(outboxId).orElse(null)
        assertNotNull(updated)
        assertEquals(OutboxStatus.FAILED, updated.status)
    }


    @Test
    fun `claim된 OutboxRecord는 다시 claim되지 않는다`() {
        // given
        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = UUID.randomUUID(),
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
            )
        )
        val outboxRecord = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObject)
        )
        repository.save(outboxRecord)

        // when - 첫 번째 claim
        val lockedBy1 = "worker-1"
        val result1 = repository.claimAndLockBatchIds(10, lockedBy1)

        // when - 두 번째 claim 시도
        val lockedBy2 = "worker-2"
        val result2 = repository.claimAndLockBatchIds(10, lockedBy2)

        // then
        assertEquals(1, result1.size)
        assertEquals(0, result2.size)
    }

    @Test
    fun `PUBLISHED된 OutboxRecord는 claim되지 않는다`() {
        // given
        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = UUID.randomUUID(),
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 15L)
            )
        )
        val outboxRecord = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObject)
        )
        repository.save(outboxRecord)

        val lockedBy = "worker-1"
        val claimResult = repository.claimAndLockBatchIds(1, lockedBy)
        repository.markPublished(claimResult.outboxInfo[0].outboxId, lockedBy)

        // when - 다시 claim 시도
        val result = repository.claimAndLockBatchIds(10, lockedBy)

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `여러 worker가 동시에 claim할 때 중복되지 않는다`() {
        // given
        val count = 10
        (1..count).forEach { i ->
            val payloadObject = ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), (i * 5).toLong())
                )
            )
            val outboxRecord = OutboxRecord.create(
                orderId = UUID.randomUUID(),
                reservationId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = objectMapper.writeValueAsString(payloadObject)
            )
            repository.save(outboxRecord)
        }

        // when
        val worker1Result = repository.claimAndLockBatchIds(5, "worker-1")
        val worker2Result = repository.claimAndLockBatchIds(5, "worker-2")

        // then
        assertEquals(5, worker1Result.size)
        assertEquals(5, worker2Result.size)

        val worker1Ids = worker1Result.outboxInfo.map { it.outboxId }.toSet()
        val worker2Ids = worker2Result.outboxInfo.map { it.outboxId }.toSet()

        assertTrue(worker1Ids.intersect(worker2Ids).isEmpty())
    }


    @Test
    fun `claim 후 상태가 PROCESSING으로 변경된다`() {
        // given
        val payloadObject = ReservationCreationSuccessPayload(
            reservationId = UUID.randomUUID(),
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 30L)
            )
        )
        val outboxRecord = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(payloadObject)
        )
        val outboxId = repository.save(outboxRecord)

        // when
        val lockedBy = "worker-1"
        repository.claimAndLockBatchIds(1, lockedBy)

        // then
        val claimed = outboxJpa.findById(outboxId).orElse(null)
        assertNotNull(claimed)
        assertEquals(OutboxStatus.PROCESSING, claimed.status)
        assertEquals(lockedBy, claimed.lockedBy)
    }
}

