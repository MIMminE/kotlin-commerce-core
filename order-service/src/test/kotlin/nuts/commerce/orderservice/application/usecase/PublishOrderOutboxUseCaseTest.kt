//package nuts.commerce.orderservice.application.usecase
//
//import nuts.commerce.orderservice.application.port.message.InMemoryMessageConsumer
//import nuts.commerce.orderservice.application.port.message.InMemoryMessageProducer
//import nuts.commerce.orderservice.application.port.message.MessageProducer
//import nuts.commerce.orderservice.application.port.repository.InMemoryOrderOutboxRepository
//import nuts.commerce.orderservice.model.infra.OutboxRecord
//import nuts.commerce.orderservice.model.infra.OutboxStatus
//import org.junit.jupiter.api.BeforeEach
//import org.mockito.kotlin.any
//import org.mockito.kotlin.doAnswer
//import org.mockito.kotlin.mock
//import org.springframework.transaction.support.TransactionCallback
//import org.springframework.transaction.support.TransactionTemplate
//import java.util.UUID
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//import kotlin.test.assertTrue
//
//class PublishOrderOutboxUseCaseTest {
//    private val orderOutboxRepository: InMemoryOrderOutboxRepository = InMemoryOrderOutboxRepository()
//    private val messageConsumer: InMemoryMessageConsumer = InMemoryMessageConsumer()
//    private val messageProducer: InMemoryMessageProducer = InMemoryMessageProducer(
//        messageConsumer,
//         failWhen = { pm -> if (pm.payload == "fail") RuntimeException("produce fail") else null }
//    )
//    private val txTemplateMock: TransactionTemplate = mock {
//        on { execute<Any?>(any()) } doAnswer { inv ->
//            val cb = inv.getArgument<TransactionCallback<Any?>>(0)
//            cb.doInTransaction(mock())
//        }
//    }
//
//    private val useCase: PublishOrderOutboxUseCase = PublishOrderOutboxUseCase(
//        orderOutboxRepository,
//        messageProducer,
//        txTemplateMock,
//        batchSize = 10,
//        maxRetries = 5
//    )
//
//    @BeforeEach
//    fun setup() {
//        orderOutboxRepository.clear()
//        messageProducer.clear()
//        messageConsumer.clear()
//    }
//
//    @Test
//    fun `발행할 대상이 없으면 produce가 호출되지 않는다`() {
//
//        useCase.publishPendingOutboxMessages()
//        assertEquals(0, messageProducer.produced.size)
//    }
//
//    @Test
//    fun `단건 정상 발행시 produced 되고 상태가 PUBLISHED 로 변경된다`() {
//
//        val record = OutboxRecord.create(UUID.randomUUID(), "OrderCreated", "{\"x\":1}")
//        orderOutboxRepository.save(record)
//
//        useCase.publishPendingOutboxMessages()
//
//        assertEquals(1, messageProducer.produced.size)
//        val produced = messageProducer.lastOrNull()
//        assertNotNull(produced)
//        assertEquals(record.outboxId, produced.eventId)
//        assertEquals(OutboxStatus.PUBLISHED, orderOutboxRepository.findById(record.id)!!.status)
//    }
//
//    @Test
//    fun `produce 중 예외 발생시 해당 레코드는 FAILED로 처리된다`() {
//
//        val success = OutboxRecord.create(UUID.randomUUID(), "T", "ok")
//        val fail = OutboxRecord.create(UUID.randomUUID(), "T", "fail")
//        orderOutboxRepository.save(success); orderOutboxRepository.save(fail)
//
//        useCase.publishPendingOutboxMessages()
//
//        assertEquals(OutboxRecord.OutboxStatus.PUBLISHED, orderOutboxRepository.findById(success.id)!!.status)
//        val failed = orderOutboxRepository.findById(fail.id)!!
//        assertEquals(OutboxRecord.OutboxStatus.FAILED, failed.status)
//        assertEquals(1, failed.retryCount)
//        assertNotNull(failed.lastError)
//        assertTrue(failed.lastError!!.contains("produce fail"))
//        assertNotNull(failed.nextRetryAt)
//    }
//
//    @Test
//    fun `재시도 초과시 DEAD 상태가 된다`() {
//
//        val rec = OutboxRecord.create(UUID.randomUUID(), "T", "x")
//        rec.markFailed(error = "pre", maxRetries = 0)
//        orderOutboxRepository.save(rec)
//
//        useCase.publishPendingOutboxMessages()
//
//        val after = orderOutboxRepository.findById(rec.id)!!
//        assertEquals(OutboxRecord.OutboxStatus.DEAD, after.status)
//        assertNotNull(after.lastError)
//    }
//
//    @Test
//    fun `produce 호출 시 status 가 PROCESSING 인 상태에서 호출된다`() {
//
//        val rec = OutboxRecord.create(UUID.randomUUID(), "T", "x")
//        orderOutboxRepository.save(rec)
//
//        val mockProducer: MessageProducer = mock {
//            on { produce(any()) } doAnswer { inv ->
//                val pm = inv.getArgument<MessageProducer.ProduceMessage>(0)
//                val managed = orderOutboxRepository.findById(pm.eventId)!!
//                assertEquals(OutboxRecord.OutboxStatus.PROCESSING, managed.status)
//                null
//            }
//        }
//
//        useCase.publishPendingOutboxMessages()
//
//        assertEquals(OutboxRecord.OutboxStatus.PUBLISHED, orderOutboxRepository.findById(rec.id)!!.status)
//    }
//}
