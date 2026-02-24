package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.inventoryservice.event.outbound.converter.ProductOutboundEvents
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.port.message.ProduceResult
import nuts.commerce.inventoryservice.port.message.ProductEventProducer
import nuts.commerce.inventoryservice.port.message.ReservationEventProducer
import nuts.commerce.inventoryservice.testutil.InMemoryOutboxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Suppress("NonAsciiCharacters")
class OutboxPublishUseCaseTest {

    private val objectMapper = ObjectMapper()
    private val outboxRepository = InMemoryOutboxRepository()
    private val immediateExecutor = Executor { command -> command.run() }

    @BeforeEach
    fun setUp() {
        outboxRepository.clear()
    }

    private fun createUseCase(
        reservationProducer: ReservationEventProducer,
        productProducer: ProductEventProducer = SuccessProductProducer(),
        reservationConverters: List<OutboundEventConverter<ReservationOutboundEvent, OutboundEventType>> = listOf(
            SuccessReservationConverter()
        ),
        productConverters: List<OutboundEventConverter<ProductOutboundEvents, OutboundEventType>> = emptyList()
    ): OutboxPublishUseCase {
        return OutboxPublishUseCase(
            outboxRepository = outboxRepository,
            reservationEventProducer = reservationProducer,
            productEventProducer = productProducer,
            outboxUpdateExecutor = immediateExecutor,
            batchSize = 2,
            claimLockedBy = "test-worker",
            reservationEventConverterList = reservationConverters,
            productEventConverterList = productConverters
        )
    }

    @Test
    fun `예약 이벤트 발행 성공 시 PUBLISHED로 전환된다`() {
        // given
        val payload = objectMapper.writeValueAsString(
            ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(record)

        val useCase = createUseCase(SuccessReservationProducer())

        // when
        useCase.execute()

        // then
        Thread.sleep(100) // 비동기 처리 대기
        val updated = outboxRepository.getAll().first { it.outboxId == record.outboxId }
        assertEquals(OutboxStatus.PUBLISHED, updated.status)
        assertNull(updated.lockedBy)
    }

    @Test
    fun `예약 이벤트 발행 실패 시 FAILED로 전환된다`() {
        // given
        val payload = objectMapper.writeValueAsString(
            ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(record)

        val useCase = createUseCase(FailureReservationProducer())

        // when
        useCase.execute()

        // then
        Thread.sleep(100) // 비동기 처리 대기
        val updated = outboxRepository.getAll().first { it.outboxId == record.outboxId }
        assertEquals(OutboxStatus.RETRY_SCHEDULED, updated.status)
        assertNull(updated.lockedBy)
        assertEquals(1, updated.attemptCount)
    }

    @Test
    fun `배치 크기만큼만 처리한다`() {
        // given
        repeat(3) {
            val payload = objectMapper.writeValueAsString(
                ReservationCreationSuccessPayload(
                    reservationId = UUID.randomUUID(),
                    reservationItemInfoList = listOf(
                        ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), (it + 1) * 10L)
                    )
                )
            )
            outboxRepository.save(
                OutboxRecord.create(
                    orderId = UUID.randomUUID(),
                    reservationId = UUID.randomUUID(),
                    idempotencyKey = UUID.randomUUID(),
                    eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                    payload = payload,
                    status = OutboxStatus.PENDING,
                    nextAttemptAt = Instant.now().minusSeconds(10)
                )
            )
        }

        val useCase = createUseCase(SuccessReservationProducer())

        // when
        useCase.execute()

        // then
        Thread.sleep(100) // 비동기 처리 대기
        val published = outboxRepository.getAll().count { it.status == OutboxStatus.PUBLISHED }
        val pending = outboxRepository.getAll().count { it.status == OutboxStatus.PENDING }
        assertEquals(2, published)
        assertEquals(1, pending)
    }


    @Test
    fun `nextAttemptAt이 미래인 레코드는 처리하지 않는다`() {
        // given
        val futurePayload = objectMapper.writeValueAsString(
            ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )
        val futureRecord = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = futurePayload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().plusSeconds(60) // 미래 시간
        )
        outboxRepository.save(futureRecord)

        val pastPayload = objectMapper.writeValueAsString(
            ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 20L)
                )
            )
        )
        val pastRecord = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = pastPayload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(pastRecord)

        val useCase = createUseCase(SuccessReservationProducer())

        // when
        useCase.execute()

        // then
        Thread.sleep(100)
        val published = outboxRepository.getAll().count { it.status == OutboxStatus.PUBLISHED }
        val pending = outboxRepository.getAll().count { it.status == OutboxStatus.PENDING }
        assertEquals(1, published) // 과거 레코드만 처리
        assertEquals(1, pending) // 미래 레코드는 그대로
    }

    @Test
    fun `3번 실패하면 FAILED 상태가 된다`() {
        // given
        val payload = objectMapper.writeValueAsString(
            ReservationCreationSuccessPayload(
                reservationId = UUID.randomUUID(),
                reservationItemInfoList = listOf(
                    ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                )
            )
        )
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            reservationId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(record)

        val useCase = createUseCase(FailureReservationProducer())

        // when - 첫 번째 실패
        useCase.execute()
        Thread.sleep(100)

        // when - 두 번째 실패
        val updated1 = outboxRepository.getAll().first()
        updated1.nextAttemptAt = Instant.now().minusSeconds(10)
        outboxRepository.save(updated1)
        useCase.execute()
        Thread.sleep(100)

        // when - 세 번째 실패
        val updated2 = outboxRepository.getAll().first()
        updated2.nextAttemptAt = Instant.now().minusSeconds(10)
        outboxRepository.save(updated2)
        useCase.execute()
        Thread.sleep(100)

        // then
        val final = outboxRepository.getAll().first()
        assertEquals(OutboxStatus.FAILED, final.status)
        assertEquals(3, final.attemptCount)
    }

    private class SuccessReservationConverter :
        OutboundEventConverter<ReservationOutboundEvent, OutboundEventType> {
        override val supportType: OutboundEventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED

        override fun convert(outboxInfo: OutboxInfo): ReservationOutboundEvent {
            return ReservationOutboundEvent(
                eventId = UUID.randomUUID(),
                outboxId = outboxInfo.outboxId,
                orderId = outboxInfo.orderId,
                eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
                payload = ReservationCreationSuccessPayload(
                    reservationId = UUID.randomUUID(),
                    reservationItemInfoList = listOf(
                        ReservationOutboundEvent.ReservationItem(UUID.randomUUID(), 10L)
                    )
                )
            )
        }
    }

    // Test Producers
    private class SuccessReservationProducer : ReservationEventProducer {
        override fun produce(outboundEvent: ReservationOutboundEvent): CompletableFuture<ProduceResult> {
            return CompletableFuture.completedFuture(
                ProduceResult.Success(
                    eventId = outboundEvent.eventId,
                    outboxId = outboundEvent.outboxId
                )
            )
        }
    }

    private class FailureReservationProducer : ReservationEventProducer {
        override fun produce(outboundEvent: ReservationOutboundEvent): CompletableFuture<ProduceResult> {
            return CompletableFuture.completedFuture(
                ProduceResult.Failure(
                    reason = "Test failure",
                    outboxId = outboundEvent.outboxId
                )
            )
        }
    }

    private class SuccessProductProducer : ProductEventProducer {
        override fun produce(outboundEvent: ProductOutboundEvent): CompletableFuture<Boolean> {
            return CompletableFuture.completedFuture(true)
        }
    }
}