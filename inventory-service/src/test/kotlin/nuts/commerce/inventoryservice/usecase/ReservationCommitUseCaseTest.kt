@file:Suppress("NonAsciiCharacters")

package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.port.repository.InMemoryReservationRepository
import nuts.commerce.inventoryservice.port.repository.InMemoryOutboxRepository
import tools.jackson.databind.ObjectMapper
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.util.UUID
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.model.OutboxEventType

class ReservationCommitUseCaseTest {
    private lateinit var reservationRepo: InMemoryReservationRepository
    private lateinit var outboxRepo: InMemoryOutboxRepository
    private lateinit var inventoryRepo: InMemoryInventoryRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var usecase: ReservationCommitUseCase

    @BeforeTest
    fun setUp() {
        reservationRepo = InMemoryReservationRepository()
        outboxRepo = InMemoryOutboxRepository()
        inventoryRepo = InMemoryInventoryRepository()
        objectMapper = ObjectMapper()
        usecase = ReservationCommitUseCase(
            inventoryRepository = inventoryRepo,
            reservationRepository = reservationRepo,
            outboxRepository = outboxRepo,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `예약을 확정하면 상태가 COMMITTED로 바뀌고 RESERVATION_COMMITTED 아웃박스가 생성되어야 한다`() {
        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        val reservation = Reservation.create(orderId = orderId, idempotencyKey = idempotency)
        val inv1 = UUID.randomUUID()
        val inv2 = UUID.randomUUID()

        inventoryRepo.save(
            inventory = Inventory.create(
                inventoryId = inv1,
                productId = UUID.randomUUID(),
                availableQuantity = 8L,
                reservationQuantity = 2L
            )
        )
        inventoryRepo.save(
            inventory = Inventory.create(
                inventoryId = inv2,
                productId = UUID.randomUUID(),
                availableQuantity = 7L,
                reservationQuantity = 3L
            )
        )

        val item1 = ReservationItem.create(reservation = reservation, inventoryId = inv1, qty = 2L)
        val item2 = ReservationItem.create(reservation = reservation, inventoryId = inv2, qty = 3L)
        reservation.addItems(listOf(item1, item2))

        reservationRepo.save(reservation)

        val result = usecase.execute(orderId)

        val saved = reservationRepo.findByOrderId(orderId)
        assertEquals(result.reservationId, saved.reservationId)
        assertEquals(saved.status.name, "COMMITTED")

        val ids = outboxRepo.claimPendingOutboxRecords(10)
        val records = outboxRepo.getOutboxRecordsListByIds(ids)
        assertEquals(1, records.size)
        val rec = records.first()
        assertEquals(OutboxEventType.RESERVATION_COMMITTED, rec.eventType)

        val payload = objectMapper.readValue(rec.payload, Map::class.java)
        assertEquals(saved.reservationId.toString(), payload["reservationId"].toString())
        val items = payload["items"] as List<*>
        assertEquals(2, items.size)
    }

    @Test
    fun `존재하지 않는 예약을 확정하면 예외를 던진다`() {
        val missingOrderId = UUID.randomUUID()
        assertFailsWith<NoSuchElementException> { usecase.execute(missingOrderId) }
    }
}

