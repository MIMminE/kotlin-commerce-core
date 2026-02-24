package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.model.OrderSaga
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.*

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaSageRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaSageRepositoryTest {

    @Autowired
    private lateinit var repository: JpaSageRepository

    companion object {
        @Suppress("unused")
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `OrderSaga를 저장하고 orderId로 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )

        // when
        val saved = repository.save(saga)
        val found = repository.findByOrderId(orderId)

        // then
        assertNotNull(saved)
        assertNotNull(found)
        assertEquals(orderId, found?.orderId)
        assertEquals(50000, found?.totalPrice)
        assertEquals("KRW", found?.currency)
    }

    @Test
    fun `OrderSageInfo를 orderId로 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 100000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        saga.reservationId = reservationId
        saga.paymentId = paymentId

        repository.save(saga)

        // when
        val sagaInfo = repository.findSageInfoByOrderId(orderId)

        // then
        assertNotNull(sagaInfo)
        assertEquals(orderId, sagaInfo?.orderId)
        assertEquals(reservationId, sagaInfo?.reservationId)
        assertEquals(paymentId, sagaInfo?.paymentId)
        assertEquals(100000, sagaInfo?.totalPrice)
        assertEquals("KRW", sagaInfo?.currency)
    }

    @Test
    fun `reservationId를 설정할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.setReservationId(orderId, reservationId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertEquals(reservationId, updated?.reservationId)
    }

    @Test
    fun `paymentId를 설정할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.setPaymentId(orderId, paymentId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertEquals(paymentId, updated?.paymentId)
    }

    @Test
    fun `예약 완료 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markReservationCompleteAt(orderId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.reservationReservedAt)
    }

    @Test
    fun `예약 해제 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markReservationReleaseAt(orderId, "cancelled")

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.reservationReleasedAt)
    }

    @Test
    fun `결제 요청 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markPaymentRequestAt(orderId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.paymentRequestedAt)
    }

    @Test
    fun `결제 완료 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markPaymentCompleteAt(orderId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.paymentCompletedAt)
    }

    @Test
    fun `결제 해제 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markPaymentReleaseAt(orderId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.paymentReleasedAt)
    }

    @Test
    fun `실패 시간과 이유를 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markFailedAt(orderId, "out of stock")

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.failedAt)
        assertEquals("out of stock", updated?.failReason)
    }

    @Test
    fun `완료 시간을 기록할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when
        repository.markCompleteAt(orderId)

        // then
        val updated = repository.findByOrderId(orderId)
        assertNotNull(updated)
        assertNotNull(updated?.completedAt)
    }

    @Test
    fun `존재하지 않는 orderId로 상태 변경 시 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalStateException> {
            repository.setReservationId(UUID.randomUUID(), UUID.randomUUID())
        }
    }

    @Test
    fun `전체 saga 생명주기를 추적할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val saga = OrderSaga.create(
            orderId = orderId,
            totalPrice = 50000,
            currency = "KRW",
            reservationRequestedAt = Instant.now()
        )
        repository.save(saga)

        // when - 예약 설정
        repository.setReservationId(orderId, reservationId)
        repository.markReservationCompleteAt(orderId)

        // when - 결제 설정
        repository.setPaymentId(orderId, paymentId)
        repository.markPaymentRequestAt(orderId)
        repository.markPaymentCompleteAt(orderId)

        // when - 완료
        repository.markCompleteAt(orderId)

        // then
        val final = repository.findByOrderId(orderId)
        assertNotNull(final)
        assertEquals(reservationId, final?.reservationId)
        assertEquals(paymentId, final?.paymentId)
        assertNotNull(final?.reservationReservedAt)
        assertNotNull(final?.paymentRequestedAt)
        assertNotNull(final?.paymentCompletedAt)
        assertNotNull(final?.completedAt)
    }
}
