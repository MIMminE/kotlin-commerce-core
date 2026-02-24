package nuts.commerce.inventoryservice.event.outbound.converter

import tools.jackson.databind.ObjectMapper
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ProductStockDecrementPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ProductEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var incrementConverter: ProductStockIncrementEventConverter
    private lateinit var decrementConverter: ProductStockDecrementEventConverter

    @BeforeTest
    fun setUp() {
        objectMapper = ObjectMapper()
        incrementConverter = ProductStockIncrementEventConverter(objectMapper)
        decrementConverter = ProductStockDecrementEventConverter(objectMapper)
    }

    @Test
    fun `ProductStockIncrementEventConverter - 단일 상품 예약 생성 이벤트를 제품 이벤트로 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val successPayload = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = quantity)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(successPayload)
        )

        // When
        val result = incrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(1, result.items.size)

        val event = result.items[0]
        assertEquals(ProductEventType.DECREMENT_STOCK, event.eventType)

        val payload = event.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload.orderId)
        assertEquals(productId, payload.productId)
        assertEquals(quantity, payload.qty)
    }

    @Test
    fun `ProductStockIncrementEventConverter - 다중 상품 예약 생성 이벤트를 제품 이벤트로 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val quantity1 = 5L
        val quantity2 = 15L

        val successPayload = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId1, qty = quantity1),
                ReservationOutboundEvent.ReservationItem(productId = productId2, qty = quantity2)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(successPayload)
        )

        // When
        val result = incrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(2, result.items.size)

        // 첫 번째 상품 이벤트 검증
        val event1 = result.items[0]
        assertEquals(ProductEventType.DECREMENT_STOCK, event1.eventType)
        val payload1 = event1.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload1.orderId)
        assertEquals(productId1, payload1.productId)
        assertEquals(quantity1, payload1.qty)

        // 두 번째 상품 이벤트 검증
        val event2 = result.items[1]
        assertEquals(ProductEventType.DECREMENT_STOCK, event2.eventType)
        val payload2 = event2.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload2.orderId)
        assertEquals(productId2, payload2.productId)
        assertEquals(quantity2, payload2.qty)
    }

    @Test
    fun `ProductStockDecrementEventConverter - 단일 상품 예약 해제 이벤트를 제품 이벤트로 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 10L

        val releasePayload = ReservationReleaseSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = quantity)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_RELEASE,
            payload = objectMapper.writeValueAsString(releasePayload)
        )

        // When
        val result = decrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(1, result.items.size)

        val event = result.items[0]
        assertEquals(ProductEventType.INCREMENT_STOCK, event.eventType)

        val payload = event.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload.orderId)
        assertEquals(productId, payload.productId)
        assertEquals(quantity, payload.qty)
    }

    @Test
    fun `ProductStockDecrementEventConverter - 다중 상품 예약 해제 이벤트를 제품 이벤트로 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()
        val quantity1 = 10L
        val quantity2 = 20L

        val releasePayload = ReservationReleaseSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId1, qty = quantity1),
                ReservationOutboundEvent.ReservationItem(productId = productId2, qty = quantity2)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_RELEASE,
            payload = objectMapper.writeValueAsString(releasePayload)
        )

        // When
        val result = decrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(2, result.items.size)

        // 첫 번째 상품 이벤트 검증
        val event1 = result.items[0]
        assertEquals(ProductEventType.INCREMENT_STOCK, event1.eventType)
        val payload1 = event1.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload1.orderId)
        assertEquals(productId1, payload1.productId)
        assertEquals(quantity1, payload1.qty)

        // 두 번째 상품 이벤트 검증
        val event2 = result.items[1]
        assertEquals(ProductEventType.INCREMENT_STOCK, event2.eventType)
        val payload2 = event2.payload as ProductStockDecrementPayload
        assertEquals(orderId, payload2.orderId)
        assertEquals(productId2, payload2.productId)
        assertEquals(quantity2, payload2.qty)
    }

    @Test
    fun `ProductStockIncrementEventConverter - 대량 상품 예약 생성 이벤트 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productIds = (1..5).map { UUID.randomUUID() }
        val quantities = listOf(1L, 2L, 3L, 4L, 5L)

        val successPayload = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = productIds.zip(quantities).map { (productId, qty) ->
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = qty)
            }
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(successPayload)
        )

        // When
        val result = incrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(5, result.items.size)

        result.items.forEachIndexed { index, event ->
            assertEquals(ProductEventType.DECREMENT_STOCK, event.eventType)
            val payload = event.payload as ProductStockDecrementPayload
            assertEquals(orderId, payload.orderId)
            assertEquals(productIds[index], payload.productId)
            assertEquals(quantities[index], payload.qty)
        }
    }

    @Test
    fun `ProductStockDecrementEventConverter - 대량 상품 예약 해제 이벤트 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productIds = (1..3).map { UUID.randomUUID() }
        val quantities = listOf(100L, 200L, 300L)

        val releasePayload = ReservationReleaseSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = productIds.zip(quantities).map { (productId, qty) ->
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = qty)
            }
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_RELEASE,
            payload = objectMapper.writeValueAsString(releasePayload)
        )

        // When
        val result = decrementConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(3, result.items.size)

        result.items.forEachIndexed { index, event ->
            assertEquals(ProductEventType.INCREMENT_STOCK, event.eventType)
            val payload = event.payload as ProductStockDecrementPayload
            assertEquals(orderId, payload.orderId)
            assertEquals(productIds[index], payload.productId)
            assertEquals(quantities[index], payload.qty)
        }
    }

    @Test
    fun `ProductStockIncrementEventConverter - 변환된 이벤트의 orderId가 원본 outboxInfo와 일치`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val successPayload = ReservationCreationSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = 5L)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_CREATION_SUCCEEDED,
            payload = objectMapper.writeValueAsString(successPayload)
        )

        // When
        val result = incrementConverter.convert(outboxInfo)

        // Then
        result.items.forEach { event ->
            val payload = event.payload as ProductStockDecrementPayload
            assertEquals(orderId, payload.orderId)
        }
    }

    @Test
    fun `ProductStockDecrementEventConverter - 변환된 이벤트의 orderId가 원본 outboxInfo와 일치`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val releasePayload = ReservationReleaseSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = 10L)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_RELEASE,
            payload = objectMapper.writeValueAsString(releasePayload)
        )

        // When
        val result = decrementConverter.convert(outboxInfo)

        // Then
        result.items.forEach { event ->
            val payload = event.payload as ProductStockDecrementPayload
            assertEquals(orderId, payload.orderId)
        }
    }
}


