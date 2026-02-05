package nuts.commerce.paymentservice.model.domain

import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaymentTest {

    @Test
    fun `startProcessing 성공`() {
        val payment = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = UUID.randomUUID(),
            money = Money(amount = 1000L, currency = "USD"),
            idempotencyKey = UUID.randomUUID()
        )

        val nowInstant = Instant.now()
        payment.startProcessing(nowInstant)

        assertEquals(PaymentStatus.PROCESSING, payment.status)
        assertEquals(nowInstant, payment.updatedAt)
    }

    @Test
    fun `startProcessing 실패 - 이미 PROCESSING 이상이면 예외`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 1000L, currency = "USD"),
            idempotencyKey = UUID.randomUUID()
        )
        payment.startProcessing(Instant.now())

        assertFailsWith<Exception> { payment.startProcessing(Instant.now()) }
    }

    @Test
    fun `승인 프로세스 성공하면 상태가 APPROVED로 변경된다`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 500L, currency = "EUR"),
            idempotencyKey = UUID.randomUUID()
        )
        val nowInstant = Instant.now()
        payment.startProcessing(nowInstant)
        payment.approve(nowInstant)

        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(nowInstant, payment.updatedAt)
    }

    @Test
    fun `승인 시 PROCESSING 상태가 아니면 예외가 발생한다`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 500L, currency = "EUR"),
            idempotencyKey = UUID.randomUUID()
        )

        assertFailsWith<Exception> { payment.approve(Instant.now()) }
    }

    @Test
    fun `거부 프로세스 성공하면 상태가 DECLINED로 변경된다`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 200L, currency = "KRW"),
            idempotencyKey = UUID.randomUUID()
        )
        val nowInstant = Instant.now()
        payment.startProcessing(nowInstant)
        payment.decline(nowInstant)

        assertEquals(PaymentStatus.DECLINED, payment.status)
        assertEquals(nowInstant, payment.updatedAt)
    }

    @Test
    fun `생성 상태에서 fail 호출하면 FAILED로 변경된다`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 200L, currency = "KRW"),
            idempotencyKey = UUID.randomUUID()
        )
        val nowInstant = Instant.now()
        payment.fail(nowInstant)

        assertEquals(PaymentStatus.FAILED, payment.status)
        assertEquals(nowInstant, payment.updatedAt)
    }

    @Test
    fun `fail 호출이 유효하지 않은 전이면 예외 발생`() {
        val payment = Payment.create(
            orderId = UUID.randomUUID(),
            money = Money(amount = 200L, currency = "KRW"),
            idempotencyKey = UUID.randomUUID()
        )
        payment.startProcessing(Instant.now())
        payment.approve(Instant.now())

        assertFailsWith<Exception> { payment.fail(Instant.now()) }
    }
}