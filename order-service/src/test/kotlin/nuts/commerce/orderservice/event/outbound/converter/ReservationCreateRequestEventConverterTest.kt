package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundReservationItem
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayloadReservation
import nuts.commerce.orderservice.model.OutboxInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ReservationCreateRequestEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var converter: ReservationCreateRequestEventConverter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        converter = ReservationCreateRequestEventConverter(objectMapper)
    }

    @Test
    fun `supportType은 RESERVATION_CREATE_REQUEST를 반환한다`() {
        // when & then
        assertEquals(OutboundEventType.RESERVATION_CREATE_REQUEST, converter.supportType)
    }

    @Test
    fun `OutboxInfo를 ReservationOutboundEvent로 변환한다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val productId1 = UUID.randomUUID()
        val productId2 = UUID.randomUUID()

        val payload = ReservationCreatePayloadReservation(
            reservationItems = listOf(
                OutboundReservationItem(
                    productId = productId1,
                    price = 5000,
                    currency = "KRW",
                    qty = 2
                ),
                OutboundReservationItem(
                    productId = productId2,
                    price = 3000,
                    currency = "KRW",
                    qty = 1
                )
            )
        )
        val payloadJson = objectMapper.writeValueAsString(payload)

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_CREATE_REQUEST,
            payload = payloadJson
        )

        // when
        val result = converter.convert(outboxInfo)

        // then
        assertNotNull(result)
        assertEquals(outboxId, result.outboxId)
        assertEquals(orderId, result.orderId)
        assertEquals(OutboundEventType.RESERVATION_CREATE_REQUEST, result.eventType)

        val resultPayload = result.payload as ReservationCreatePayloadReservation
        assertEquals(2, resultPayload.reservationItems.size)

        val item1 = resultPayload.reservationItems[0]
        assertEquals(productId1, item1.productId)
        assertEquals(5000, item1.price)
        assertEquals("KRW", item1.currency)
        assertEquals(2, item1.qty)

        val item2 = resultPayload.reservationItems[1]
        assertEquals(productId2, item2.productId)
        assertEquals(3000, item2.price)
        assertEquals("KRW", item2.currency)
        assertEquals(1, item2.qty)
    }
}

