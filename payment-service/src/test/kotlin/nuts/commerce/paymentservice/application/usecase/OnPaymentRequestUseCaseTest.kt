package nuts.commerce.paymentservice.application.usecase

import nuts.commerce.paymentservice.application.port.payment.InMemoryPaymentProvider
import nuts.commerce.paymentservice.application.port.repository.InMemoryPaymentRepository
import nuts.commerce.paymentservice.application.port.payment.PaymentProvider
import nuts.commerce.paymentservice.model.domain.Money
import nuts.commerce.paymentservice.model.domain.Payment
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OnPaymentRequestUseCaseTest {

    private val provider = InMemoryPaymentProvider(defaultSuccess = true)
    private val repo = InMemoryPaymentRepository()
    private val useCase = OnPaymentRequestUseCase(paymentProvider = provider, paymentRepository = repo)

    @BeforeEach
    fun setup() {
        provider.clearResponses()
        repo.clear()
    }

    @Test
    fun `결제 요청 성공 - 프로바이더가 승인하면 결제 상태가 APPROVED로 변경된다`() {
        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        // enqueue success response
        provider.enqueueResponse(
            PaymentProvider.ChargeResponse(success = true, providerPaymentId = "prov-1", failureReason = null)
        )

        val cmd = OnPaymentRequestUseCase.Command(
            orderId = orderId,
            amount = 1000L,
            currency = "USD",
            paymentMethod = "CARD",
            idempotencyKey = idempotency
        )

        val result = useCase.handle(cmd)

        assertEquals("APPROVED", result.status)
        val saved = repo.findById(result.paymentId)
        assertNotNull(saved)
        assertEquals("APPROVED", saved.status().name)
    }

    @Test
    fun `프로바이더 실패 응답 - 결제가 DECLINED 상태가 된다`() {
        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        provider.enqueueResponse(
            PaymentProvider.ChargeResponse(success = false, providerPaymentId = null, failureReason = "insufficient")
        )

        val cmd = OnPaymentRequestUseCase.Command(
            orderId = orderId,
            amount = 500L,
            currency = "USD",
            paymentMethod = "CARD",
            idempotencyKey = idempotency
        )

        val result = useCase.handle(cmd)

        assertEquals("DECLINED", result.status)
        val saved = repo.findByOrderId(orderId)
        assertNotNull(saved)
        assertEquals("DECLINED", saved.status().name)
    }

    @Test
    fun `프로바이더 예외 발생 - 결과는 FAILED, 저장된 결제 상태는 DECLINED`() {
        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        // create a provider that throws
        val throwingProvider = object : PaymentProvider {
            override fun charge(request: PaymentProvider.ChargeRequest): PaymentProvider.ChargeResponse {
                throw RuntimeException("provider down")
            }
        }

        val localUseCase = OnPaymentRequestUseCase(paymentProvider = throwingProvider, paymentRepository = repo)

        val cmd = OnPaymentRequestUseCase.Command(
            orderId = orderId,
            amount = 200L,
            currency = "USD",
            paymentMethod = "CARD",
            idempotencyKey = idempotency
        )

        val result = localUseCase.handle(cmd)

        assertEquals("FAILED", result.status)
        val saved = repo.findByOrderId(orderId)
        assertNotNull(saved)
        // on exception we call decline(), which sets DECLINED
        assertEquals("DECLINED", saved.status().name)
    }

    @Test
    fun `중복 요청(idempotency) - 기존 결제가 있으면 해당 결제가 반환된다`() {
        val orderId = UUID.randomUUID()
        val existing = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = orderId,
            money = Money(amount = 300L, currency = "USD"),
            idempotencyKey = UUID.randomUUID()
        )

        // mark approved to simulate prior success
        existing.startProcessing(java.time.Instant.now())
        existing.approve(java.time.Instant.now(), providerPaymentId = "prov-xyz")

        repo.save(existing)

        val cmd = OnPaymentRequestUseCase.Command(
            orderId = orderId,
            amount = 300L,
            currency = "USD",
            paymentMethod = "CARD",
            idempotencyKey = UUID.randomUUID()
        )

        val result = useCase.handle(cmd)

        assertEquals(existing.paymentId(), result.paymentId)
        assertEquals(existing.status().name, result.status)
    }
}
