package nuts.commerce.orderservice.application.usecase

import nuts.commerce.orderservice.application.port.message.InMemoryMessageProducer
import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
import nuts.commerce.orderservice.model.infra.OutboxEventType
import nuts.commerce.orderservice.model.infra.OutboxRecord
import nuts.commerce.orderservice.model.infra.OutboxStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals


@Suppress("NonAsciiCharacters")
class PublishReserveInventoryOutboxTest {

    private lateinit var outboxRepository: InMemoryOrderOutboxRepository
    private lateinit var messageProducer: InMemoryMessageProducer
    private lateinit var txManager: PlatformTransactionManager
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var useCase: PublishOrderOutboxUseCase
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        outboxRepository = InMemoryOrderOutboxRepository()
        messageProducer = InMemoryMessageProducer()
        txManager = object : PlatformTransactionManager {
            override fun getTransaction(definition: TransactionDefinition?): TransactionStatus =
                SimpleTransactionStatus()

            override fun commit(status: TransactionStatus) {}
            override fun rollback(status: TransactionStatus) {}
        }
        txTemplate = TransactionTemplate(txManager)
        objectMapper = ObjectMapper()
        useCase = PublishOrderOutboxUseCase(
            orderOutboxRepository = outboxRepository,
            messageProducer = messageProducer,
            transactionTemplate = txTemplate,
            batchSize = 10,
            maxRetries = 3
        )
        outboxRepository.clear()
    }

    @Test
    fun `재고_예약_요청_이벤트는_퍼블리시되어야_한다`() {
        val et = OutboxEventType.RESERVE_INVENTORY_REQUEST
        val id = UUID.randomUUID()
        val agg = UUID.randomUUID()
        val payloadObj = mapOf("orderId" to agg.toString(), "items" to listOf(mapOf("productId" to "p-1", "qty" to 2)))
        val payload = objectMapper.writeValueAsString(payloadObj)
        val rec = OutboxRecord.create(id = id, aggregateId = agg, eventType = et, payload = payload)
        outboxRepository.save(rec)

        useCase.publishPendingOutboxMessages()

        val managed = outboxRepository.findById(id)!!
        assertEquals(OutboxStatus.PUBLISHED, managed.status)
        val produced = messageProducer.produced
        assertEquals(1, produced.size)
        assertEquals(et.name, produced[0].eventType)
        assertEquals(payload, produced[0].payload)
        assertEquals(agg, produced[0].aggregateId)
    }

    @Test
    fun 결제_요청_이벤트는_퍼블리시되어야_한다() {
        val et = OutboxEventType.PAYMENT_REQUEST
        val id = UUID.randomUUID()
        val agg = UUID.randomUUID()
        val payloadObj = mapOf("orderId" to agg.toString(), "amount" to 1500L, "currency" to "KRW")
        val payload = objectMapper.writeValueAsString(payloadObj)
        val rec = OutboxRecord.create(id = id, aggregateId = agg, eventType = et, payload = payload)
        outboxRepository.save(rec)

        useCase.publishPendingOutboxMessages()

        val managed = outboxRepository.findById(id)!!
        assertEquals(OutboxStatus.PUBLISHED, managed.status)
        val produced = messageProducer.produced
        assertEquals(1, produced.size)
        assertEquals(et.name, produced[0].eventType)
        assertEquals(payload, produced[0].payload)
        assertEquals(agg, produced[0].aggregateId)
    }

    @Test
    fun `재고_예약_확정_이벤트는_퍼블리시되어야_한다`() {
        val et = OutboxEventType.RESERVE_INVENTORY_CONFIRM
        val id = UUID.randomUUID()
        val agg = UUID.randomUUID()
        val payloadObj = mapOf("reservationId" to UUID.randomUUID().toString())
        val payload = objectMapper.writeValueAsString(payloadObj)
        val rec = OutboxRecord.create(id = id, aggregateId = agg, eventType = et, payload = payload)
        outboxRepository.save(rec)

        useCase.publishPendingOutboxMessages()

        val managed = outboxRepository.findById(id)!!
        assertEquals(OutboxStatus.PUBLISHED, managed.status)
        val produced = messageProducer.produced
        assertEquals(1, produced.size)
        assertEquals(et.name, produced[0].eventType)
        assertEquals(payload, produced[0].payload)
        assertEquals(agg, produced[0].aggregateId)
    }

    @Test
    fun `재고_예약_반환_이벤트는_퍼블리시되어야_한다`() {
        val et = OutboxEventType.RESERVE_INVENTORY_RELEASE
        val id = UUID.randomUUID()
        val agg = UUID.randomUUID()
        val payloadObj = mapOf("orderId" to agg.toString(), "reason" to "payment_failed")
        val payload = objectMapper.writeValueAsString(payloadObj)
        val rec = OutboxRecord.create(id = id, aggregateId = agg, eventType = et, payload = payload)
        outboxRepository.save(rec)

        useCase.publishPendingOutboxMessages()

        val managed = outboxRepository.findById(id)!!
        assertEquals(OutboxStatus.PUBLISHED, managed.status)
        val produced = messageProducer.produced
        assertEquals(1, produced.size)
        assertEquals(et.name, produced[0].eventType)
        assertEquals(payload, produced[0].payload)
        assertEquals(agg, produced[0].aggregateId)
    }
}

