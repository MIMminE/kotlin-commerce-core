package nuts.commerce.paymentservice.event.inbound.handler

import nuts.commerce.paymentservice.adapter.payment.InMemoryPaymentProvider
import nuts.commerce.paymentservice.adapter.repository.InMemoryOutboxRepository
import nuts.commerce.paymentservice.adapter.repository.InMemoryPaymentRepository
import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentCreatePayload
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.model.PaymentStatus
import nuts.commerce.paymentservice.testutil.TestTransactionTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class PaymentCreateRequestHandlerTest {

    private lateinit var paymentRepository: InMemoryPaymentRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var paymentProvider: InMemoryPaymentProvider
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: PaymentCreateRequestHandler

    @BeforeEach
    fun setup() {
        paymentRepository = InMemoryPaymentRepository()
        outboxRepository = InMemoryOutboxRepository()
        paymentProvider = InMemoryPaymentProvider()
        objectMapper = ObjectMapper()
        txTemplate = TestTransactionTemplate()

        handler = PaymentCreateRequestHandler(
            paymentRepository = paymentRepository,
            outboxRepository = outboxRepository,
            paymentProvider = paymentProvider,
            objectMapper = objectMapper,
            txTemplate = txTemplate
        )
    }

    @Test
    fun `결제 생성 요청이면 결제가 APPROVED 상태가 된다`() {
        val event = buildCreateEvent()

        handler.handle(event)

        val paymentId = paymentRepository.findPaymentIdForIdempotencyKey(event.orderId, event.eventId)
        assertNotNull(paymentId)
        val payment = paymentRepository.findById(paymentId)
        assertNotNull(payment)
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(paymentProvider.providerName, payment.paymentProvider)
        assertNotNull(payment.providerPaymentId)
    }

    @Test
    fun `결제 생성 요청이면 아웃박스가 저장된다`() {
        val event = buildCreateEvent()

        handler.handle(event)

        val claimed = outboxRepository.claimAndLockBatchIds(batchSize = 10, lockedBy = "test-worker")
        assertEquals(1, claimed.size)
        val info = claimed.outboxInfo.first()
        assertEquals(event.orderId, info.orderId)
        assertEquals(OutboundEventType.PAYMENT_CREATION_SUCCEEDED, info.eventType)

        val payload = objectMapper.readValue(info.payload, PaymentCreationSuccessPayload::class.java)
        assertEquals(paymentProvider.providerName, payload.paymentProvider)
    }

    private fun buildCreateEvent(): PaymentInboundEvent {
        val orderId = UUID.randomUUID()
        return PaymentInboundEvent(
            eventId = UUID.randomUUID(),
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_CREATE_REQUEST,
            payload = PaymentCreatePayload(amount = 10_000L, currency = "KRW")
        )
    }
}

