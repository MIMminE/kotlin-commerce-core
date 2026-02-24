package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.model.ReservationStatus
import nuts.commerce.inventoryservice.port.repository.ReservationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.util.UUID

@Suppress("NonAsciiCharacters")
@DataJpaTest
@Import(JpaReservationRepository::class, JpaReservationItemRepository::class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JpaReservationRepositoryTest {

    @Autowired
    lateinit var repository: ReservationRepository

    @Autowired
    lateinit var reservationJpa: ReservationJpa

    @BeforeEach
    fun clear() {
        reservationJpa.deleteAll()
    }

    companion object {
        @ServiceConnection
        @Container
        val db = PostgreSQLContainer(DockerImageName.parse("postgres:15.3-alpine"))
    }

    @Test
    fun `새로운 Reservation을 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val reservationItem = ReservationItem.create(productId = productId, qty = quantity)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(listOf(reservationItem))

        // when
        val saved = repository.save(reservation)

        // then
        assertNotNull(saved)
        assertEquals(orderId, saved.orderId)
        assertEquals(idempotencyKey, saved.idempotencyKey)
        assertEquals(ReservationStatus.CREATED, saved.status)
        assertEquals(1, saved.items.size)
        assertEquals(productId, saved.items[0].productId)
        assertEquals(quantity, saved.items[0].qty)
    }


    @Test
    fun `orderId와 idempotencyKey로 ReservationInfo를 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()

        val item1 = ReservationItem.create(productId = productId1, qty = 10L)
        val item2 = ReservationItem.create(productId = productId2, qty = 20L)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(listOf(item1, item2))
        repository.save(reservation)

        // when
        val reservationInfo = repository.findReservationIdForIdempotencyKey(orderId, idempotencyKey)

        // then
        assertNotNull(reservationInfo)
        assertEquals(2, reservationInfo?.reservationItemInfos?.size)
        assertTrue(reservationInfo?.reservationItemInfos?.any { it.productId == productId1 && it.quantity == 10L } == true)
        assertTrue(reservationInfo?.reservationItemInfos?.any { it.productId == productId2 && it.quantity == 20L } == true)
    }


    @Test
    fun `동일한 orderId와 idempotencyKey로 저장하려고 하면 실패한다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()

        val item1 = ReservationItem.create(productId = UUID.randomUUID(), qty = 10L)
        val reservation1 = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )

        val item2 = ReservationItem.create(productId = UUID.randomUUID(), qty = 20L)
        val reservation2 = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )

        // when
        reservation1.addItems(listOf(item1))
        repository.save(reservation1)

        // then
        assertThrows<DataIntegrityViolationException> {
            reservation2.addItems(listOf(item2))
            repository.save(reservation2)
        }
    }

    @Test
    fun `Reservation 상태를 CREATED에서 COMMITTED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val item = ReservationItem.create(productId = UUID.randomUUID(), qty = 10L)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(listOf(item))
        val saved = repository.save(reservation)

        // when
        val found = repository.findById(saved.reservationId)
        assertNotNull(found)
        found!!.confirm()
        repository.save(found)

        // then
        val updated = repository.findById(saved.reservationId)
        assertEquals(ReservationStatus.COMMITTED, updated?.status)
    }

    @Test
    fun `Reservation 상태를 CREATED에서 RELEASED로 변경할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val item = ReservationItem.create(productId = UUID.randomUUID(), qty = 10L)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(listOf(item))
        val saved = repository.save(reservation)

        // when
        val found = repository.findById(saved.reservationId)
        assertNotNull(found)
        found!!.release()
        repository.save(found)

        // then
        val updated = repository.findById(saved.reservationId)
        assertEquals(ReservationStatus.RELEASED, updated?.status)
    }

    @Test
    fun `다중 ReservationItem을 포함한 Reservation을 저장할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productIds = (1..5).map { UUID.randomUUID() }
        val items = productIds.mapIndexed { index, productId ->
            ReservationItem.create(productId = productId, qty = ((index + 1) * 10).toLong())
        }

        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(items)

        // when
        val saved = repository.save(reservation)

        // then
        val found = repository.findById(saved.reservationId)
        assertNotNull(found)
        assertEquals(5, found?.items?.size)
    }

    @Test
    fun `ReservationInfo 조회 시 모든 ReservationItem이 포함된다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val productId3 = UUID.randomUUID()

        val items = listOf(
            ReservationItem.create(productId = productId1, qty = 5L),
            ReservationItem.create(productId = productId2, qty = 10L),
            ReservationItem.create(productId = productId3, qty = 15L)
        )

        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(items)
        val saved = repository.save(reservation)

        // when
        val reservationInfo = repository.findReservationInfo(saved.reservationId)

        // then
        assertNotNull(reservationInfo)
        assertEquals(3, reservationInfo?.reservationItemInfos?.size)
        assertEquals(5L, reservationInfo?.reservationItemInfos?.find { it.productId == productId1 }?.quantity)
        assertEquals(10L, reservationInfo?.reservationItemInfos?.find { it.productId == productId2 }?.quantity)
        assertEquals(15L, reservationInfo?.reservationItemInfos?.find { it.productId == productId3 }?.quantity)
    }

    @Test
    fun `같은 orderId에 여러 Reservation을 저장하고 각각 조회할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()

        val item1 = ReservationItem.create(productId = UUID.randomUUID(), qty = 10L)
        val reservation1 = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey1,
        )
        reservation1.addItems(listOf(item1))

        val item2 = ReservationItem.create(productId = UUID.randomUUID(), qty = 20L)
        val reservation2 = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey2,
        )
        reservation2.addItems(listOf(item2))

        // when
        val saved1 = repository.save(reservation1)
        val saved2 = repository.save(reservation2)

        // then
        val found1 = repository.findById(saved1.reservationId)
        val found2 = repository.findById(saved2.reservationId)

        assertNotNull(found1)
        assertNotNull(found2)
        assertEquals(10L, found1?.items?.get(0)?.qty)
        assertEquals(20L, found2?.items?.get(0)?.qty)
    }

    @Test
    fun `Reservation의 낙관적 잠금 Version 동작을 확인할 수 있다`() {
        // given
        val orderId = UUID.randomUUID()
        val idempotencyKey = UUID.randomUUID()
        val item = ReservationItem.create(productId = UUID.randomUUID(), qty = 10L)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = idempotencyKey,
        )
        reservation.addItems(listOf(item))
        val saved = repository.save(reservation)

        // when - 첫 번째 업데이트
        val reservation1 = repository.findById(saved.reservationId)
        assertNotNull(reservation1)
        reservation1!!.confirm()
        repository.save(reservation1)

        // when - 두 번째 조회
        val reservation2 = repository.findById(saved.reservationId)

        // then - 버전이 증가했는지 확인
        assertNotNull(reservation2)
        assertEquals(ReservationStatus.COMMITTED, reservation2?.status)
        assertNotNull(reservation2?.version)
        assertTrue((reservation2?.version ?: 0) > 0)
    }

}