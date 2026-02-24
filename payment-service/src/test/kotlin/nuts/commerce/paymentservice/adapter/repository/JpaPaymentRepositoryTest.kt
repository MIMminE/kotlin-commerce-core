package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.Money
import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.model.PaymentStatus
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaPaymentRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaPaymentRepositoryTest {

    @Autowired
    lateinit var repository: PaymentRepository

    @Autowired
    lateinit var paymentJpa: PaymentJpa

    @BeforeEach
    fun clear() {
        paymentJpa.deleteAll()
    }

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }


    @Test
    fun `새로운 Payment를 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 10000L, currency = "KRW")
        val payment = Payment.create(
            paymentId = paymentId,
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money
        )

        // when
        val savedId = repository.save(payment)

        // then
        assertEquals(paymentId, savedId)
        val saved = repository.findById(savedId)
        assertNotNull(saved)
        assertEquals(orderId, saved?.orderId)
        assertEquals(idempotencyKey, saved?.idempotencyKey)
        assertEquals(PaymentStatus.CREATED, saved?.status)
        assertEquals(money.amount, saved?.money?.amount)
        assertEquals(money.currency, saved?.money?.currency)
    }

    @Test
    fun `동일한 orderId와 idempotencyKey로 저장하려고 하면 실패한다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money1 = Money(amount = 10000L, currency = "KRW")
        val money2 = Money(amount = 20000L, currency = "KRW")

        val payment1 = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money1
        )

        val payment2 = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money2
        )

        // when
        repository.save(payment1)

        // then - 유니크 제약 조건 위반
        assertThrows<DataIntegrityViolationException> {
            repository.save(payment2)
        }
    }

    @Test
    fun `서로 다른 idempotencyKey는 같은 orderId로도 여러 개 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()
        val money = Money(amount = 10000L, currency = "KRW")

        val payment1 = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = orderId,
            idempotencyKey = idempotencyKey1,
            status = PaymentStatus.CREATED,
            money = money
        )

        val payment2 = Payment.create(
            paymentId = UUID.randomUUID(),
            orderId = orderId,
            idempotencyKey = idempotencyKey2,
            status = PaymentStatus.CREATED,
            money = money
        )

        // when
        val id1 = repository.save(payment1)
        val id2 = repository.save(payment2)

        // then
        assertNotEquals(id1, id2)
        assertNotNull(repository.findById(id1))
        assertNotNull(repository.findById(id2))
    }


    @Test
    fun `paymentId로 providerPaymentId를 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 25000L, currency = "KRW")
        val providerPaymentId = UUID.randomUUID()

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.APPROVED,
            money = money,
            paymentProvider = "TOSS",
            providerPaymentId = providerPaymentId
        )
        val paymentId = repository.save(payment)

        // when
        val found = repository.getProviderPaymentIdByPaymentId(paymentId)

        // then
        assertNotNull(found)
        assertEquals(providerPaymentId, found)
    }


    @Test
    fun `Payment의 상태를 CREATED에서 APPROVED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 100000L, currency = "KRW")
        val providerPaymentId = UUID.randomUUID()

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money
        )
        val paymentId = repository.save(payment)

        // when
        val found = repository.findById(paymentId)
        assertNotNull(found)
        found!!.approve("TOSS", providerPaymentId)
        repository.save(found)

        // then
        val updated = repository.findById(paymentId)
        assertNotNull(updated)
        assertEquals(PaymentStatus.APPROVED, updated?.status)
        assertEquals("TOSS", updated?.paymentProvider)
        assertEquals(providerPaymentId, updated?.providerPaymentId)
    }

    @Test
    fun `Payment의 상태를 APPROVED에서 COMMITED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 50000L, currency = "KRW")
        val providerPaymentId = UUID.randomUUID()

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.APPROVED,
            money = money,
            paymentProvider = "TOSS",
            providerPaymentId = providerPaymentId
        )
        val paymentId = repository.save(payment)

        // when
        val found = repository.findById(paymentId)
        assertNotNull(found)
        found!!.commit()
        repository.save(found)

        // then
        val updated = repository.findById(paymentId)
        assertNotNull(updated)
        assertEquals(PaymentStatus.COMMITED, updated?.status)
    }

    @Test
    fun `Payment의 상태를 CREATED에서 FAILED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 75000L, currency = "KRW")

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money
        )
        val paymentId = repository.save(payment)

        // when
        val found = repository.findById(paymentId)
        assertNotNull(found)
        found!!.fail("Insufficient funds")
        repository.save(found)

        // then
        val updated = repository.findById(paymentId)
        assertNotNull(updated)
        assertEquals(PaymentStatus.FAILED, updated?.status)
    }

    @Test
    fun `Payment의 상태를 APPROVED에서 RELEASED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 120000L, currency = "KRW")
        val providerPaymentId = UUID.randomUUID()

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.APPROVED,
            money = money,
            paymentProvider = "TOSS",
            providerPaymentId = providerPaymentId
        )
        val paymentId = repository.save(payment)

        // when
        val found = repository.findById(paymentId)
        assertNotNull(found)
        found!!.release()
        repository.save(found)

        // then
        val updated = repository.findById(paymentId)
        assertNotNull(updated)
        assertEquals(PaymentStatus.RELEASED, updated?.status)
    }


    @Test
    fun `같은 orderId에 여러 Payment를 저장하고 각각 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()
        val money1 = Money(amount = 10000L, currency = "KRW")
        val money2 = Money(amount = 20000L, currency = "KRW")

        val payment1 = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey1,
            status = PaymentStatus.CREATED,
            money = money1
        )
        val payment2 = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey2,
            status = PaymentStatus.CREATED,
            money = money2
        )

        // when
        val id1 = repository.save(payment1)
        val id2 = repository.save(payment2)

        // then
        val found1 = repository.findById(id1)
        val found2 = repository.findById(id2)

        assertNotNull(found1)
        assertNotNull(found2)
        assertEquals(money1.amount, found1?.money?.amount)
        assertEquals(money2.amount, found2?.money?.amount)
    }

    @Test
    fun `Payment의 낙관적 잠금 Version 동작을 확인할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val money = Money(amount = 55000L, currency = "KRW")
        val providerPaymentId = UUID.randomUUID()

        val payment = Payment.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.CREATED,
            money = money
        )
        val paymentId = repository.save(payment)

        // when - 첫 번째 업데이트
        var payment1 = repository.findById(paymentId)
        assertNotNull(payment1)
        payment1!!.approve("TOSS", providerPaymentId)
        val version1 = payment1.version
        repository.save(payment1)

        // when - 두 번째 업데이트
        var payment2 = repository.findById(paymentId)
        assertNotNull(payment2)
        payment2!!.commit()
        repository.save(payment2)

        // then - 버전이 증가했는지 확인
        val final = repository.findById(paymentId)
        assertNotNull(final)
        assertEquals(PaymentStatus.COMMITED, final?.status)
        assertNotNull(final?.version)
        assertTrue(final?.version ?: 0 > 0)
    }
}