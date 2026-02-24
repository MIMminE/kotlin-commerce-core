package nuts.commerce.inventoryservice.event.inbound.handler

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.ReservationReleasePayload
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.model.Reservation
import nuts.commerce.inventoryservice.model.ReservationItem
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.model.ReservationStatus
import nuts.commerce.inventoryservice.testutil.InMemoryInventoryRepository
import nuts.commerce.inventoryservice.testutil.InMemoryOutboxRepository
import nuts.commerce.inventoryservice.testutil.InMemoryReservationItemRepository
import nuts.commerce.inventoryservice.testutil.InMemoryReservationRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("NonAsciiCharacters")
class ReservationReleaseHandlerTest {

    private lateinit var inventoryRepository: InMemoryInventoryRepository
    private lateinit var reservationRepository: InMemoryReservationRepository
    private lateinit var reservationItemRepository: InMemoryReservationItemRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: ReservationReleaseHandler

    @BeforeTest
    fun setUp() {
        inventoryRepository = InMemoryInventoryRepository()
        reservationRepository = InMemoryReservationRepository()
        reservationItemRepository = InMemoryReservationItemRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = ReservationReleaseHandler(
            inventoryRepository = inventoryRepository,
            reservationRepository = reservationRepository,
            outboxRepository = outboxRepository,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `예약 해제 성공 - CREATED 상태에서 해제`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        // 인벤토리 생성 및 예약
        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 100L
        )
        inventoryRepository.save(inventory)
        inventoryRepository.reserveInventory(productId, quantity)

        // 예약 생성
        val reservationIdempotencyKey = UUID.randomUUID()
        val reservationItem = ReservationItem.create(productId = productId, qty = quantity)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = reservationIdempotencyKey,
            status = ReservationStatus.CREATED,
        )
        reservation.addItems(listOf(reservationItem))
        reservationRepository.save(reservation)

        val releasePayload = ReservationReleasePayload(reservationId = reservation.reservationId)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = releasePayload
        )

        // When
        handler.handle(event)

        // Then
        // 1. 예약 상태가 RELEASED로 변경되었는지 확인
        val updatedReservation = reservationRepository.findById(reservation.reservationId)
        assertNotNull(updatedReservation)
        assertEquals(ReservationStatus.RELEASED, updatedReservation.status)

        // 2. 인벤토리 수량이 원래대로 복구되었는지 확인
        val currentInventories = inventoryRepository.getAllCurrentInventory()
        assertEquals(1, currentInventories.size)
        assertEquals(100L, currentInventories[0].availableQuantity)

        // 3. 아웃박스 레코드가 생성되었는지 확인
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)
        assertEquals(OutboundEventType.RESERVATION_RELEASE, outboxRecords[0].eventType)
        assertEquals(OutboxStatus.PENDING, outboxRecords[0].status)
        assertEquals(orderId, outboxRecords[0].orderId)
        assertEquals(reservation.reservationId, outboxRecords[0].reservationId)
    }


    @Test
    fun `예약 해제 성공 - 다중 상품 예약 해제`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val quantity1 = 5L
        val quantity2 = 20L

        // 인벤토리 생성 및 예약
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()
        val inventory1 = Inventory.create(
            idempotencyKey = idempotencyKey1,
            productId = productId1,
            productName = "상품1",
            availableQuantity = 100L
        )
        val inventory2 = Inventory.create(
            idempotencyKey = idempotencyKey2,
            productId = productId2,
            productName = "상품2",
            availableQuantity = 200L
        )
        inventoryRepository.save(inventory1)
        inventoryRepository.save(inventory2)
        inventoryRepository.reserveInventory(productId1, quantity1)
        inventoryRepository.reserveInventory(productId2, quantity2)

        // 예약 생성
        val reservationIdempotencyKey = UUID.randomUUID()
        val reservationItem1 = ReservationItem.create(productId = productId1, qty = quantity1)
        val reservationItem2 = ReservationItem.create(productId = productId2, qty = quantity2)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = reservationIdempotencyKey,
            status = ReservationStatus.CREATED,
        )
        reservation.addItems(listOf(reservationItem1, reservationItem2))
        reservationRepository.save(reservation)

        val releasePayload = ReservationReleasePayload(reservationId = reservation.reservationId)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = releasePayload
        )

        // When
        handler.handle(event)

        // Then
        // 1. 예약 상태가 RELEASED로 변경되었는지 확인
        val updatedReservation = reservationRepository.findById(reservation.reservationId)
        assertNotNull(updatedReservation)
        assertEquals(ReservationStatus.RELEASED, updatedReservation.status)

        // 2. 모든 인벤토리 수량이 원래대로 복구되었는지 확인
        val currentInventories = inventoryRepository.getAllCurrentInventory()
        assertEquals(2, currentInventories.size)
        val inv1 = currentInventories.find { it.productId == productId1 }!!
        val inv2 = currentInventories.find { it.productId == productId2 }!!
        assertEquals(100L, inv1.availableQuantity)
        assertEquals(200L, inv2.availableQuantity)

        // 3. 아웃박스 레코드가 생성되었는지 확인
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)
        assertEquals(OutboundEventType.RESERVATION_RELEASE, outboxRecords[0].eventType)
        assertTrue(outboxRecords[0].payload.contains(quantity1.toString()))
        assertTrue(outboxRecords[0].payload.contains(quantity2.toString()))
    }

    @Test
    fun `예약 해제 성공 - 아웃박스 페이로드 검증`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 25L

        // 인벤토리 생성 및 예약
        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 100L
        )
        inventoryRepository.save(inventory)
        inventoryRepository.reserveInventory(productId, quantity)

        // 예약 생성
        val reservationIdempotencyKey = UUID.randomUUID()
        val reservationItem = ReservationItem.create(productId = productId, qty = quantity)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = reservationIdempotencyKey,
            status = ReservationStatus.CREATED,
        )
        reservation.addItems(listOf(reservationItem))
        reservationRepository.save(reservation)


        val releasePayload = ReservationReleasePayload(reservationId = reservation.reservationId)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = releasePayload
        )

        // When
        handler.handle(event)

        // Then
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)

        val outboxRecord = outboxRecords[0]
        assertEquals(orderId, outboxRecord.orderId)
        assertEquals(reservation.reservationId, outboxRecord.reservationId)
        assertEquals(eventId, outboxRecord.idempotencyKey)
        assertTrue(outboxRecord.payload.contains("productId"))
        assertTrue(outboxRecord.payload.contains(quantity.toString()))
    }

    @Test
    fun `예약 해제 성공 - 부분 해제 불가능 (전체 해제만 가능)`() {
        // 이 테스트는 모든 예약 아이템이 함께 해제되는지 확인
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val quantity1 = 10L
        val quantity2 = 15L

        // 인벤토리 생성 및 예약
        val idempotencyKey1 = UUID.randomUUID()
        val idempotencyKey2 = UUID.randomUUID()
        val inventory1 = Inventory.create(
            idempotencyKey = idempotencyKey1,
            productId = productId1,
            productName = "상품1",
            availableQuantity = 100L
        )
        val inventory2 = Inventory.create(
            idempotencyKey = idempotencyKey2,
            productId = productId2,
            productName = "상품2",
            availableQuantity = 200L
        )
        inventoryRepository.save(inventory1)
        inventoryRepository.save(inventory2)
        inventoryRepository.reserveInventory(productId1, quantity1)
        inventoryRepository.reserveInventory(productId2, quantity2)

        // 예약 생성
        val reservationIdempotencyKey = UUID.randomUUID()
        val reservationItem1 = ReservationItem.create(productId = productId1, qty = quantity1)
        val reservationItem2 = ReservationItem.create(productId = productId2, qty = quantity2)
        val reservation = Reservation.create(
            orderId = orderId,
            idempotencyKey = reservationIdempotencyKey,
            status = ReservationStatus.CREATED,
        )
        reservation.addItems(listOf(reservationItem1, reservationItem2))
        reservationRepository.save(reservation)

        // When
        val releasePayload = ReservationReleasePayload(reservationId = reservation.reservationId)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = releasePayload
        )

        handler.handle(event)

        // Then
        // 모든 상품이 함께 해제되어야 함
        val currentInventories = inventoryRepository.getAllCurrentInventory()
        val inv1 = currentInventories.find { it.productId == productId1 }!!
        val inv2 = currentInventories.find { it.productId == productId2 }!!
        assertEquals(100L, inv1.availableQuantity)  // 모두 복구됨
        assertEquals(200L, inv2.availableQuantity)  // 모두 복구됨
    }

    @Test
    fun `예약 해제 실패 - 존재하지 않는 예약ID`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val nonExistentReservationId = UUID.randomUUID()

        val releasePayload = ReservationReleasePayload(reservationId = nonExistentReservationId)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = releasePayload
        )

        // When & Then
        try {
            handler.handle(event)
            assertEquals(expected = true, actual = false)  // 예외가 발생해야 함
        } catch (ex: IllegalArgumentException) {
            assertEquals(ex.message?.contains("Invalid reservation id"), true)
        }
    }
}

