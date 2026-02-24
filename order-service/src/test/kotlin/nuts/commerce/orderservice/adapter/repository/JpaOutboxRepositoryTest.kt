package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundReservationItem
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayloadReservation
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.model.OutboxStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.*

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaOutboxRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaOutboxRepositoryTest {

    @Autowired
    private lateinit var repository: JpaOutboxRepository

    @Autowired
    private lateinit var outboxJpa: OrderOutboxJpa

    private val objectMapper = ObjectMapper()

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `OutboxRecord를 저장하고 outboxId를 반환한다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = listOf(
                OutboundReservationItem(
                    productId = UUID.randomUUID(),
                    price = 10000,
                    currency = "KRW",
                    qty = 1
                )
            )
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = idempotencyKey,
            payload = objectMapper.writeValueAsString(payload)
        )

        // when
        val savedId = repository.save(record)

        // then
        assertEquals(outboxId, savedId)
    }

    @Test
    fun `PENDING 상태의 OutboxRecord를 클레임할 수 있다`() {
        // given
        val now = Instant.now()
        val outboxId1 = UUID.randomUUID()
        val outboxId2 = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val reservationPayload = ReservationCreatePayloadReservation(
            reservationItems = listOf(
                OutboundReservationItem(
                    productId = UUID.randomUUID(),
                    price = 5000,
                    currency = "KRW",
                    qty = 2
                )
            )
        )

        val paymentPayload = PaymentCreatePayload(
            amount = 10000,
            currency = "KRW"
        )

        val record1 = OutboxRecord.create(
            outboxId = outboxId1,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(reservationPayload),
            nextAttemptAt = now.minusSeconds(10)
        )

        val record2 = OutboxRecord.create(
            outboxId = outboxId2,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(paymentPayload),
            nextAttemptAt = now.minusSeconds(5)
        )

        repository.save(record1)
        repository.save(record2)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 2, lockedBy = "worker1")

        // then
        assertEquals(2, result.size)
        assertEquals(2, result.outboxInfo.size)
    }

    @Test
    fun `nextAttemptAt이 미래인 레코드는 클레임되지 않는다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = listOf(
                OutboundReservationItem(
                    productId = UUID.randomUUID(),
                    price = 10000,
                    currency = "KRW",
                    qty = 1
                )
            )
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            nextAttemptAt = now.plusSeconds(60)  // 미래 시간
        )

        repository.save(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker1")

        // then
        assertEquals(0, result.size)
        assertEquals(0, result.outboxInfo.size)
    }

    @Test
    fun `PROCESSING 상태의 레코드는 클레임되지 않는다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = emptyList()
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.PROCESSING,
            lockedBy = "other-worker",
            lockedUntil = now.plusSeconds(300),
            nextAttemptAt = now.minusSeconds(10)
        )

        repository.save(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker1")

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `batchSize만큼만 클레임된다`() {
        // given
        val now = Instant.now()
        val orderId = UUID.randomUUID()

        // 5개의 PENDING 레코드 생성
        repeat(5) { index ->
            val payload = PaymentCreatePayload(
                amount = (index + 1) * 10000L,
                currency = "KRW"
            )

            val record = OutboxRecord.create(
                outboxId = UUID.randomUUID(),
                orderId = orderId,
                eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
                idempotencyKey = UUID.randomUUID(),
                payload = objectMapper.writeValueAsString(payload),
                nextAttemptAt = now.minusSeconds((5 - index).toLong())
            )
            repository.save(record)
        }

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 3, lockedBy = "worker1")

        // then
        assertEquals(3, result.size)
        assertEquals(3, result.outboxInfo.size)
    }

    @Test
    fun `클레임된 OutboxRecord를 PUBLISHED 상태로 변경할 수 있다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = listOf(
                OutboundReservationItem(
                    productId = UUID.randomUUID(),
                    price = 25000,
                    currency = "KRW",
                    qty = 3
                )
            )
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker1",
            lockedUntil = now.plusSeconds(1000)
        )

        repository.save(record)

        // when
        repository.markPublished(outboxId, "worker1")

        val updatedRecord = outboxJpa.findById(outboxId)
            .orElseThrow { IllegalStateException("OutboxRecord not found for outboxId: $outboxId") }
        assertEquals(OutboxStatus.PUBLISHED, updatedRecord.status)
        assertNull(updatedRecord.lockedBy)
        assertNull(updatedRecord.lockedUntil)
    }

    @Test
    fun `클레임된 OutboxRecord를 FAILED 상태로 변경할 수 있다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = PaymentCreatePayload(
            amount = 50000,
            currency = "KRW"
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker1",
            lockedUntil = now.plusSeconds(1000)
        )

        repository.save(record)

        // when
        repository.markFailed(outboxId, "worker1")


        // then
        val updatedRecord = outboxJpa.findById(outboxId)
            .orElseThrow { IllegalStateException("OutboxRecord not found for outboxId: $outboxId") }
        assertEquals(OutboxStatus.FAILED, updatedRecord.status)
        assertNull(updatedRecord.lockedBy)
        assertNull(updatedRecord.lockedUntil)
    }

    @Test
    fun `다른 worker가 잠금한 레코드는 상태 변경할 수 없다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = emptyList()
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.PROCESSING,
            lockedBy = "worker1",
            lockedUntil = now.plusSeconds(300)
        )

        repository.save(record)

        // when & then - 다른 worker가 시도하면 예외 발생
        assertThrows<IllegalStateException> {
            repository.markPublished(outboxId, "worker2")
        }
    }


    @Test
    fun `PUBLISHED 상태의 레코드는 조회되지 않는다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = PaymentCreatePayload(
            amount = 100000,
            currency = "KRW"
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.PUBLISHED,
            nextAttemptAt = now.minusSeconds(10)
        )

        repository.save(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker1")

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `FAILED 상태의 레코드는 조회되지 않는다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()

        val payload = PaymentCreatePayload(
            amount = 75000,
            currency = "KRW"
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            status = OutboxStatus.FAILED,
            nextAttemptAt = now.minusSeconds(10)
        )

        repository.save(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker1")

        // then
        assertEquals(0, result.size)
    }

    @Test
    fun `클레임된 OutboxInfo에 올바른 정보가 포함된다`() {
        // given
        val now = Instant.now()
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val eventType = OutboundEventType.PAYMENT_CREATE_REQUEST

        val payload = PaymentCreatePayload(
            amount = 50000,
            currency = "KRW"
        )

        val record = OutboxRecord.create(
            outboxId = outboxId,
            orderId = orderId,
            eventType = eventType,
            idempotencyKey = UUID.randomUUID(),
            payload = objectMapper.writeValueAsString(payload),
            nextAttemptAt = now.minusSeconds(10)
        )

        repository.save(record)

        // when
        val result = repository.claimAndLockBatchIds(batchSize = 10, lockedBy = "worker1")

        // then
        assertEquals(1, result.size)
        val outboxInfo = result.outboxInfo[0]
        assertEquals(outboxId, outboxInfo.outboxId)
        assertEquals(orderId, outboxInfo.orderId)
        assertEquals(eventType, outboxInfo.eventType)
        assertEquals(objectMapper.writeValueAsString(payload), outboxInfo.payload)
    }

    @Test
    fun `여러 OutboxRecord를 순차적으로 처리할 수 있다`() {
        // given
        val now = Instant.now()
        val orderId = UUID.randomUUID()
        val outboxIds = mutableListOf<UUID>()

        // 3개의 PENDING 레코드 생성
        repeat(3) { index ->
            val outboxId = UUID.randomUUID()
            outboxIds.add(outboxId)

            val payload = PaymentCreatePayload(
                amount = (index + 1) * 20000L,
                currency = "KRW"
            )

            val record = OutboxRecord.create(
                outboxId = outboxId,
                orderId = orderId,
                eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
                idempotencyKey = UUID.randomUUID(),
                payload = objectMapper.writeValueAsString(payload),
                nextAttemptAt = now.minusSeconds((3 - index).toLong())
            )
            repository.save(record)
        }

        // when - 첫 번째 배치 클레임
        val result1 = repository.claimAndLockBatchIds(batchSize = 2, lockedBy = "worker1")
        assertEquals(2, result1.size)

        // 첫 번째 배치의 첫 번째 레코드를 PUBLISHED로 변경
        repository.markPublished(outboxIds[0], "worker1")

        // when - 두 번째 배치 클레임
        val result2 = repository.claimAndLockBatchIds(batchSize = 2, lockedBy = "worker2")

        // then
        assertEquals(1, result2.size)
        assertEquals(outboxIds[2], result2.outboxInfo[0].outboxId)
    }
}