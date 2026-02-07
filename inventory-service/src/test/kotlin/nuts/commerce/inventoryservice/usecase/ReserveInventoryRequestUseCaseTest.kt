@file:Suppress("NonAsciiCharacters")

package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.repository.InMemoryInventoryRepository
import nuts.commerce.inventoryservice.port.repository.InMemoryReservationRepository
import nuts.commerce.inventoryservice.port.repository.InMemoryOutboxRepository
import tools.jackson.databind.ObjectMapper
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.UUID

class ReserveInventoryRequestUseCaseTest {
    private lateinit var inventoryRepo: InMemoryInventoryRepository
    private lateinit var reservationRepo: InMemoryReservationRepository
    private lateinit var outboxRepo: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var usecase: ReservationRequestUseCase

    @BeforeTest
    fun setUp() {
        inventoryRepo = InMemoryInventoryRepository()
        reservationRepo = InMemoryReservationRepository()
        outboxRepo = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        usecase = ReservationRequestUseCase(
            inventoryRepository = inventoryRepo,
            reservationRepository = reservationRepo,
            outboxRepository = outboxRepo,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `예약 생성 시 Reservation과 Outbox가 생성되어야 한다`() {
        val invId1 = UUID.randomUUID()
        val invId2 = UUID.randomUUID()

        inventoryRepo.save(
            nuts.commerce.inventoryservice.model.Inventory.create(
                inventoryId = invId1,
                productId = UUID.randomUUID(),
                availableQuantity = 10L
            )
        )
        inventoryRepo.save(
            nuts.commerce.inventoryservice.model.Inventory.create(
                inventoryId = invId2,
                productId = UUID.randomUUID(),
                availableQuantity = 5L
            )
        )

        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        val cmd = ReservationRequestUseCase.Command(
            orderId = orderId,
            idempotencyKey = idempotency,
            items = listOf(
                ReservationRequestUseCase.Command.Item(invId1, 3L),
                ReservationRequestUseCase.Command.Item(invId2, 2L)
            )
        )

        val result = usecase.execute(cmd)

        val saved = reservationRepo.findByOrderId(orderId)
        assertEquals(result.reservationId, saved.reservationId)
        assertEquals(2, saved.items.size)

        val updated1 = inventoryRepo.findById(invId1)
        val updated2 = inventoryRepo.findById(invId2)
        assertEquals(7L, updated1.quantity)
        assertEquals(3L, updated2.quantity)

        val records = outboxRepo.getOutboxRecordsListByIds(outboxRepo.claimPendingOutboxRecords(10))
        assertEquals(1, records.size)
        val payload = objectMapper.readValue(records[0].payload, Map::class.java)
        assertEquals(saved.reservationId.toString(), payload["reservationId"].toString())
        val items = payload["items"] as List<*>
        assertEquals(2, items.size)
    }

    @Test
    fun `멱등성 동일 요청 재호출 시 기존 예약을 반환해야 한다`() {
        val invId = UUID.randomUUID()
        inventoryRepo.save(
            nuts.commerce.inventoryservice.model.Inventory.create(
                inventoryId = invId,
                productId = UUID.randomUUID(),
                availableQuantity = 10L
            )
        )

        val orderId = UUID.randomUUID()
        val idempotency = UUID.randomUUID()

        val cmd = ReservationRequestUseCase.Command(
            orderId = orderId,
            idempotencyKey = idempotency,
            items = listOf(ReservationRequestUseCase.Command.Item(invId, 2L))
        )
        val r1 = usecase.execute(cmd)

        val r2 = usecase.execute(cmd)
        assertEquals(r1.reservationId, r2.reservationId)

        val updated = inventoryRepo.findById(invId)
        assertEquals(8L, updated.quantity)
    }
}
