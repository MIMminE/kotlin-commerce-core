//@file:Suppress("NonAsciiCharacters")
//
//package nuts.commerce.inventoryservice.usecase
//
//import nuts.commerce.inventoryservice.port.message.InMemoryQuantityUpdateEventProducer
//import nuts.commerce.inventoryservice.port.repository.InMemoryOutboxRepository
//import tools.jackson.databind.ObjectMapper
//import kotlin.test.BeforeTest
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import java.util.UUID
//import nuts.commerce.inventoryservice.port.message.QuantityUpdateEvent
//import nuts.commerce.inventoryservice.model.OutboxRecord
//import nuts.commerce.inventoryservice.model.OutboxEventType
//
//class PublishOutboxUseCaseTest {
//    private lateinit var outboxRepo: InMemoryOutboxRepository
//    private lateinit var producer: InMemoryQuantityUpdateEventProducer
//    private lateinit var objectMapper: ObjectMapper
//    private lateinit var usecase: OutboxPublishUseCase
//
//    @BeforeTest
//    fun setUp() {
//        outboxRepo = InMemoryOutboxRepository()
//        producer = InMemoryQuantityUpdateEventProducer()
//        objectMapper = ObjectMapper()
//        usecase = OutboxPublishUseCase(
//            outboxRepository = outboxRepo,
//            producer = producer,
//            objectMapper = objectMapper,
//            batchSize = 10
//        )
//    }
//
//    @Test
//    fun `퍼블리시하면 페이로드가 프로듀서로 전송되고 레코드는 PUBLISHED 되어야 한다`() {
//        val outboxId = UUID.randomUUID()
//        val payloadEvent = QuantityUpdateEvent(productId = UUID.randomUUID(), quantity = 42L)
//
//        val record = OutboxRecord.createWithPayload(
//            reservationId = UUID.randomUUID(),
//            eventType = OutboxEventType.INVENTORY_UPDATED,
//            payloadObj = payloadEvent,
//            objectMapper = objectMapper,
//            outboxId = outboxId
//        )
//
//        outboxRepo.save(record)
//
//        usecase.execute()
//
//        val produced = producer.producedFor(outboxId)
//        assertEquals(1, produced.size)
//        val producedEvent = produced[0]
//        assertEquals(payloadEvent.productId, producedEvent.productId)
//        assertEquals(payloadEvent.quantity, producedEvent.quantity)
//
//        val recAfter = outboxRepo.getOutboxRecordsListByIds(listOf(outboxId)).first()
//        assertEquals(recAfter.status.name, "PUBLISHED")
//    }
//
//    @Test
//    fun `잘못된 페이로드면 FAILED 처리되어야 한다`() {
//        val badOutboxId = UUID.randomUUID()
//        val record = OutboxRecord.createWithPayload(
//            reservationId = UUID.randomUUID(),
//            eventType = OutboxEventType.INVENTORY_UPDATED,
//            payloadObj = "not-a-json",
//            objectMapper = objectMapper,
//            outboxId = badOutboxId
//        )
//        outboxRepo.save(record)
//
//        usecase.execute()
//
//        val recAfter = outboxRepo.getOutboxRecordsListByIds(listOf(badOutboxId)).first()
//        assertEquals(recAfter.status.name, "FAILED")
//    }
//}
