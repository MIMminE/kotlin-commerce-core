package nuts.commerce.paymentservice.event.inbound.handler

import nuts.commerce.paymentservice.adapter.payment.InMemoryPaymentProvider
import nuts.commerce.paymentservice.testutil.InMemoryOutboxRepository
import nuts.commerce.paymentservice.testutil.InMemoryPaymentRepository
import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentConfirmPayload
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentConfirmSuccessPayload
import nuts.commerce.paymentservice.model.Money
import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.model.PaymentStatus
import nuts.commerce.paymentservice.port.payment.CreatePaymentRequest
import nuts.commerce.paymentservice.port.payment.CreatePaymentResult
import nuts.commerce.paymentservice.testutil.TestTransactionTemplate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class PaymentConfirmRequestHandlerTest {

    private lateinit var paymentRepository: InMemoryPaymentRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var paymentProvider: InMemoryPaymentProvider
    private lateinit var txTemplate: TransactionTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: PaymentConfirmRequestHandler

    @BeforeEach
    fun setup() {
        paymentRepository = InMemoryPaymentRepository()
        outboxRepository = InMemoryOutboxRepository()
        paymentProvider = InMemoryPaymentProvider()
        objectMapper = ObjectMapper()
        txTemplate = TestTransactionTemplate()

        handler = PaymentConfirmRequestHandler(
            paymentRepository = paymentRepository,
            outboxRepository = outboxRepository,
            paymentProvider = paymentProvider,
            txTemplate = txTemplate,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `결제 확정 요청이면 결제 상태가 COMMITED로 변경된다`() {
        val (event, paymentId) = prepareEventWithApprovedPayment()

        handler.handle(event)

        val updated = paymentRepository.findById(paymentId)
        assertNotNull(updated)
        assertEquals(PaymentStatus.COMMITED, updated.status)
    }

    @Test
    fun `결제 확정 요청이면 아웃박스가 저장된다`() {
        val (event, _) = prepareEventWithApprovedPayment()

        handler.handle(event)

        val claimed = outboxRepository.claimAndLockBatchIds(batchSize = 10, lockedBy = "test-worker")
        assertEquals(1, claimed.size)
        val info = claimed.outboxInfo.first()
        assertEquals(event.orderId, info.orderId)
        assertEquals((event.payload as PaymentConfirmPayload).paymentId, info.paymentId)
        assertEquals(OutboundEventType.PAYMENT_CONFIRM, info.eventType)
    }

    @Test
    fun `결제 확정 요청이면 아웃박스 payload에 프로바이더 결제 정보가 담긴다`() {
        val (event, _) = prepareEventWithApprovedPayment()

        handler.handle(event)

        val claimed = outboxRepository.claimAndLockBatchIds(batchSize = 10, lockedBy = "test-worker")
        val info = claimed.outboxInfo.first()
        val outboxPayload = objectMapper.readValue(info.payload, PaymentConfirmSuccessPayload::class.java)
        assertEquals(paymentProvider.providerName, outboxPayload.paymentProvider)
        assertEquals(paymentProviderIdFor(event), outboxPayload.providerPaymentId)
    }

    private fun prepareEventWithApprovedPayment(): Pair<PaymentInboundEvent, UUID> {
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        val providerPaymentId = createProviderPayment(orderId, paymentId)

        val payment = Payment.create(
            paymentId = paymentId,
            orderId = orderId,
            idempotencyKey = UUID.randomUUID(),
            status = PaymentStatus.APPROVED,
            money = Money(10_000L, "KRW"),
            paymentProvider = paymentProvider.providerName,
            providerPaymentId = providerPaymentId
        )
        paymentRepository.save(payment)

        return PaymentInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = PaymentConfirmPayload(paymentId = paymentId)
        ) to paymentId
    }

    private fun createProviderPayment(orderId: UUID, paymentId: UUID): UUID {
        val createResult = paymentProvider.createPayment(
            CreatePaymentRequest(
                orderId = orderId,
                paymentId = paymentId,
                amount = 10_000L,
                currency = "KRW"
            )
        ).get()

        val providerPaymentId = (createResult as? CreatePaymentResult.Success)
            ?.providerPaymentId
        assertNotNull(providerPaymentId)
        return providerPaymentId
    }

    private fun paymentProviderIdFor(event: PaymentInboundEvent): UUID {
        val payload = event.payload as PaymentConfirmPayload
        val payment = paymentRepository.findById(payload.paymentId)
        return requireNotNull(payment?.providerPaymentId)
    }
}
