package nuts.commerce.inventoryservice.event.outbound.converter

import tools.jackson.databind.ObjectMapper
import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.model.OutboxInfo
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ReservationEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var createEventConverter: ReservationCreateEventConverter
    private lateinit var confirmEventConverter: ReservationConfirmEventConverter
    private lateinit var createFailEventConverter: ReservationCreateFailEventConverter
    private lateinit var releaseEventConverter: ReservationReleaseEventConverter

    @BeforeTest
    fun setUp() {
        objectMapper = ObjectMapper()
        createEventConverter = ReservationCreateEventConverter(objectMapper)
        confirmEventConverter = ReservationConfirmEventConverter(objectMapper)
        createFailEventConverter = ReservationCreateFailEventConverter(objectMapper)
        releaseEventConverter = ReservationReleaseEventConverter(objectMapper)
    }

    @Test
    fun `ReservationCreateEventConverter - 예약 생성 성공 이벤트 변환`() {
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
        val result = createEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.orderId)
        assertEquals(outboxId, result.outboxId)
        assertEquals(OutboundEventType.RESERVATION_CREATION_SUCCEEDED, result.eventType)
        assertEquals(reservationId, (result.payload as ReservationCreationSuccessPayload).reservationId)
    }

    @Test
    fun `ReservationCreateEventConverter - 다중 상품 예약 성공 이벤트 변환`() {
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
        val result = createEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        val payload = result.payload as ReservationCreationSuccessPayload
        assertEquals(2, payload.reservationItemInfoList.size)
        assertEquals(productId1, payload.reservationItemInfoList[0].productId)
        assertEquals(quantity1, payload.reservationItemInfoList[0].qty)
        assertEquals(productId2, payload.reservationItemInfoList[1].productId)
        assertEquals(quantity2, payload.reservationItemInfoList[1].qty)
    }

    @Test
    fun `ReservationConfirmEventConverter - 예약 확정 성공 이벤트 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 20L

        val confirmPayload = ReservationConfirmSuccessPayload(
            reservationId = reservationId,
            reservationItemInfoList = listOf(
                ReservationOutboundEvent.ReservationItem(productId = productId, qty = quantity)
            )
        )

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = reservationId,
            eventType = OutboundEventType.RESERVATION_CONFIRM,
            payload = objectMapper.writeValueAsString(confirmPayload)
        )

        // When
        val result = confirmEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.orderId)
        assertEquals(outboxId, result.outboxId)
        assertEquals(OutboundEventType.RESERVATION_CONFIRM, result.eventType)
        assertEquals(reservationId, (result.payload as ReservationConfirmSuccessPayload).reservationId)
    }

    @Test
    fun `ReservationCreateFailEventConverter - 예약 생성 실패 이벤트 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val failReason = "재고 부족으로 인한 예약 실패"

        val failPayload = ReservationCreationFailPayload(reason = failReason)

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            reservationId = null,
            eventType = OutboundEventType.RESERVATION_CREATION_FAILED,
            payload = objectMapper.writeValueAsString(failPayload)
        )

        // When
        val result = createFailEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.orderId)
        assertEquals(outboxId, result.outboxId)
        assertEquals(OutboundEventType.RESERVATION_CREATION_FAILED, result.eventType)
        assertEquals(failReason, (result.payload as ReservationCreationFailPayload).reason)
    }

    @Test
    fun `ReservationReleaseEventConverter - 예약 해제 성공 이벤트 변환`() {
        // Given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val quantity = 25L

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
        val result = releaseEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.orderId)
        assertEquals(outboxId, result.outboxId)
        assertEquals(OutboundEventType.RESERVATION_RELEASE, result.eventType)
        assertEquals(reservationId, (result.payload as ReservationReleaseSuccessPayload).reservationId)
    }

    @Test
    fun `ReservationReleaseEventConverter - 다중 상품 해제 이벤트 변환`() {
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
        val result = releaseEventConverter.convert(outboxInfo)

        // Then
        assertNotNull(result)
        val payload = result.payload as ReservationReleaseSuccessPayload
        assertEquals(2, payload.reservationItemInfoList.size)
    }
}