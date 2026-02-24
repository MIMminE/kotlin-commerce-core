package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.OutboxStatus
import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import nuts.commerce.paymentservice.testutil.InMemoryOutboxRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@Suppress("NonAsciiCharacters")
class PublishOutboxUseCaseTest {

    private val objectMapper = ObjectMapper()
    private val outboxRepository = InMemoryOutboxRepository()
    private val immediateExecutor = Executor { command -> command.run() }

    private fun createUseCase(
        producer: PaymentEventProducer,
        converters: List<OutboundEventConverter> = listOf(SuccessConverter())
    ): PublishOutboxUseCase {
        return PublishOutboxUseCase(
            outboxRepository = outboxRepository,
            paymentEventProducer = producer,
            outboxUpdateExecutor = immediateExecutor,
            batchSize = 2,
            claimLockedBy = "test-worker",
            paymentEventConverterList = converters
        )
    }

    @Test
    fun `claim 후 publish 하면 PUBLISHED로 전환된다`() {
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(record)

        val useCase = createUseCase(SuccessProducer())
        val claim = useCase.claim()
        useCase.publish(claim)

        val updated = outboxRepositorySnapshot(record.outboxId)
        assertEquals(OutboxStatus.PUBLISHED, updated.status)
        assertNull(updated.lockedBy)
    }

    @Test
    fun `프로듀서 실패 시 FAILED로 전환된다`() {
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        val record = OutboxRecord.create(
            orderId = UUID.randomUUID(),
            paymentId = UUID.randomUUID(),
            idempotencyKey = UUID.randomUUID(),
            eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
            payload = payload,
            status = OutboxStatus.PENDING,
            nextAttemptAt = Instant.now().minusSeconds(10)
        )
        outboxRepository.save(record)

        val useCase = createUseCase(FailureProducer())
        val claim = useCase.claim()
        useCase.publish(claim)

        val updated = outboxRepositorySnapshot(record.outboxId)
        assertEquals(OutboxStatus.FAILED, updated.status)
        assertNull(updated.lockedBy)
    }

    @Test
    fun `배치 크기만큼만 claim 한다`() {
        repeat(3) {
            val payload = objectMapper.writeValueAsString(mapOf("index" to it))
            outboxRepository.save(
                OutboxRecord.create(
                    orderId = UUID.randomUUID(),
                    paymentId = UUID.randomUUID(),
                    idempotencyKey = UUID.randomUUID(),
                    eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                    payload = payload,
                    status = OutboxStatus.PENDING,
                    nextAttemptAt = Instant.now().minusSeconds(10)
                )
            )
        }

        val useCase = createUseCase(SuccessProducer())
        val claim = useCase.claim()
        useCase.publish(claim)

        val published = listAllSnapshots().count { it.status == OutboxStatus.PUBLISHED }
        val pending = listAllSnapshots().count { it.status == OutboxStatus.PENDING }
        assertEquals(2, published)
        assertEquals(1, pending)
    }

    @Test
    fun `컨버터가 없으면 예외를 던진다`() {
        val payload = objectMapper.writeValueAsString(mapOf("paymentProvider" to "TOSS"))
        outboxRepository.save(
            OutboxRecord.create(
                orderId = UUID.randomUUID(),
                paymentId = UUID.randomUUID(),
                idempotencyKey = UUID.randomUUID(),
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = payload,
                status = OutboxStatus.PENDING,
                nextAttemptAt = Instant.now().minusSeconds(10)
            )
        )

        val useCase = createUseCase(SuccessProducer(), converters = emptyList())
        val claim = useCase.claim()

        val error = assertThrows(IllegalStateException::class.java) {
            useCase.publish(claim)
        }
        assertTrue(error.message!!.contains("No converter found"))
    }

    private fun outboxRepositorySnapshot(outboxId: UUID): OutboxRecord {
        val field = InMemoryOutboxRepository::class.java.getDeclaredField("records")
        field.isAccessible = true
        val map = field.get(outboxRepository) as LinkedHashMap<UUID, OutboxRecord>
        return map[outboxId] ?: throw IllegalStateException("Record not found")
    }

    private fun listAllSnapshots(): List<OutboxRecord> {
        val field = InMemoryOutboxRepository::class.java.getDeclaredField("records")
        field.isAccessible = true
        val map = field.get(outboxRepository) as LinkedHashMap<UUID, OutboxRecord>
        return map.values.toList()
    }

    private class SuccessConverter : OutboundEventConverter {
        override val supportType: OutboundEventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED
        override fun convert(outboxInfo: OutboxInfo): PaymentOutboundEvent {
            return PaymentOutboundEvent(
                outboxId = outboxInfo.outboxId,
                orderId = outboxInfo.orderId,
                paymentId = outboxInfo.paymentId,
                eventType = OutboundEventType.PAYMENT_CREATION_SUCCEEDED,
                payload = PaymentCreationSuccessPayload(paymentProvider = "TOSS")
            )
        }
    }

    private class SuccessProducer : PaymentEventProducer {
        override fun produce(paymentOutboundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult> {
            return CompletableFuture.completedFuture(
                ProduceResult.Success(
                    eventId = paymentOutboundEvent.eventId,
                    outboxId = paymentOutboundEvent.outboxId
                )
            )
        }
    }

    private class FailureProducer : PaymentEventProducer {
        override fun produce(paymentOutboundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult> {
            return CompletableFuture.completedFuture(
                ProduceResult.Failure(
                    reason = "failure",
                    outboxId = paymentOutboundEvent.outboxId
                )
            )
        }
    }
}