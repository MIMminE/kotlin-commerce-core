package nuts.commerce.inventoryservice.event.inbound.handler

import nuts.commerce.inventoryservice.event.inbound.InboundEventType
import nuts.commerce.inventoryservice.event.inbound.InboundReservationItem
import nuts.commerce.inventoryservice.event.inbound.ReservationCreatePayload
import nuts.commerce.inventoryservice.event.inbound.ReservationInboundEvent
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.model.Inventory
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.testutil.InMemoryInventoryRepository
import nuts.commerce.inventoryservice.testutil.InMemoryOutboxRepository
import nuts.commerce.inventoryservice.testutil.InMemoryReservationRepository
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("NonAsciiCharacters")
class ReservationCreateHandlerTest {

    private lateinit var inventoryRepository: InMemoryInventoryRepository
    private lateinit var reservationRepository: InMemoryReservationRepository
    private lateinit var outboxRepository: InMemoryOutboxRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: ReservationCreateHandler

    @BeforeTest
    fun setUp() {
        inventoryRepository = InMemoryInventoryRepository()
        reservationRepository = InMemoryReservationRepository()
        outboxRepository = InMemoryOutboxRepository()
        objectMapper = ObjectMapper()
        handler = ReservationCreateHandler(
            inventoryRepository = inventoryRepository,
            reservationRepository = reservationRepository,
            outboxRepository = outboxRepository,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `예약 생성 성공 - 인벤토리 예약 및 아웃박스 생성`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 100L
        )
        inventoryRepository.save(inventory)

        val requestItems = listOf(
            InboundReservationItem(productId = productId, qty = quantity)
        )
        val payload = ReservationCreatePayload(requestItem = requestItems)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payload
        )

        // When
        handler.handle(event)

        // Then
        // 1. 예약이 생성되었는지 확인
        val reservationInfo = reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
        assertNotNull(reservationInfo)
        assertEquals(1, reservationInfo.reservationItemInfos.size)
        assertEquals(productId, reservationInfo.reservationItemInfos[0].productId)
        assertEquals(quantity, reservationInfo.reservationItemInfos[0].quantity)

        // 2. 인벤토리가 예약되었는지 확인
        val currentInventories = inventoryRepository.getAllCurrentInventory()
        assertEquals(1, currentInventories.size)
        assertEquals(100L - quantity, currentInventories[0].availableQuantity)

        // 3. 아웃박스 레코드가 생성되었는지 확인
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)
        assertEquals(OutboundEventType.RESERVATION_CREATION_SUCCEEDED, outboxRecords[0].eventType)
        assertEquals(OutboxStatus.PENDING, outboxRecords[0].status)
        assertEquals(orderId, outboxRecords[0].orderId)
    }

    @Test
    fun `예약 생성 실패 - 인벤토리 부족`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 5L  // 부족한 수량
        )
        inventoryRepository.save(inventory)

        val requestItems = listOf(
            InboundReservationItem(productId = productId, qty = quantity)
        )
        val payload = ReservationCreatePayload(requestItem = requestItems)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payload
        )

        // When
        handler.handle(event)

        // Then
        // 1. 실패 아웃박스 레코드가 생성되었는지 확인
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)
        assertEquals(OutboundEventType.RESERVATION_CREATION_FAILED, outboxRecords[0].eventType)
        assertEquals(OutboxStatus.PENDING, outboxRecords[0].status)

        // 2. 예약이 생성되지 않았는지 확인
        val reservationInfo = reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
        assertEquals(null, reservationInfo)

        // 3. 인벤토리가 변경되지 않았는지 확인
        val currentInventories = inventoryRepository.getAllCurrentInventory()
        assertEquals(5L, currentInventories[0].availableQuantity)
    }

    @Test
    fun `예약 생성 성공 - 다중 상품 예약`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val quantity1 = 5L
        val quantity2 = 10L

        // 인벤토리 초기화
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

        val requestItems = listOf(
            InboundReservationItem(productId = productId1, qty = quantity1),
            InboundReservationItem(productId = productId2, qty = quantity2)
        )
        val payload = ReservationCreatePayload(requestItem = requestItems)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payload
        )

        // When
        handler.handle(event)

        // Then
        // 1. 예약이 생성되었는지 확인
        val reservationInfo = reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
        assertNotNull(reservationInfo)
        assertEquals(2, reservationInfo.reservationItemInfos.size)

        // 2. 아웃박스 레코드가 생성되었는지 확인
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)
        assertEquals(OutboundEventType.RESERVATION_CREATION_SUCCEEDED, outboxRecords[0].eventType)
    }

    @Test
    fun `예약 생성 성공 - 예약 정보 조회`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 20L

        // 인벤토리 초기화
        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 100L
        )
        inventoryRepository.save(inventory)

        val requestItems = listOf(
            InboundReservationItem(productId = productId, qty = quantity)
        )
        val payload = ReservationCreatePayload(requestItem = requestItems)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payload
        )

        // When
        handler.handle(event)

        // Then
        val reservationInfo = reservationRepository.findReservationIdForIdempotencyKey(orderId, eventId)
        assertNotNull(reservationInfo)

        val reservationById = reservationRepository.findReservationInfo(reservationInfo.reservationId)
        assertNotNull(reservationById)
        assertEquals(reservationInfo.reservationId, reservationById.reservationId)
        assertEquals(1, reservationById.reservationItemInfos.size)
    }

    @Test
    fun `예약 생성 성공 - 아웃박스 페이로드 검증`() {
        // Given
        val orderId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 15L

        val idempotencyKey = UUID.randomUUID()
        val inventory = Inventory.create(
            idempotencyKey = idempotencyKey,
            productId = productId,
            productName = "테스트 상품",
            availableQuantity = 100L
        )
        inventoryRepository.save(inventory)

        val requestItems = listOf(
            InboundReservationItem(productId = productId, qty = quantity)
        )
        val payload = ReservationCreatePayload(requestItem = requestItems)
        val event = ReservationInboundEvent(
            eventId = eventId,
            orderId = orderId,
            eventType = InboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payload
        )

        // When
        handler.handle(event)

        // Then
        val outboxRecords = outboxRepository.getAll()
        assertEquals(1, outboxRecords.size)

        val outboxRecord = outboxRecords[0]
        assertEquals(orderId, outboxRecord.orderId)
        assertNotNull(outboxRecord.reservationId)
        assertEquals(eventId, outboxRecord.idempotencyKey)
        assertTrue(outboxRecord.payload.contains("productId"))
        assertTrue(outboxRecord.payload.contains(quantity.toString()))
    }
}