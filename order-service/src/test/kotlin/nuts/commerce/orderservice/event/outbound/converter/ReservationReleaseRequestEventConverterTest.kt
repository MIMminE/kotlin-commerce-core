package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayloadReservation
import nuts.commerce.orderservice.model.OutboxInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class ReservationReleaseRequestEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var converter: ReservationReleaseRequestEventConverter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        converter = ReservationReleaseRequestEventConverter(objectMapper)
    }

    @Test
    fun `supportType은 RESERVATION_RELEASE_REQUEST를 반환한다`() {
        // when & then
        assertEquals(OutboundEventType.RESERVATION_RELEASE_REQUEST, converter.supportType)
    }

    @Test
    fun `OutboxInfo를 ReservationOutboundEvent로 변환한다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val reservationId = UUID.randomUUID()

        val payload = ReservationReleasePayloadReservation(reservationId = reservationId)
        val payloadJson = objectMapper.writeValueAsString(payload)

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.RESERVATION_RELEASE_REQUEST,
            payload = payloadJson
        )

        // when
        val result = converter.convert(outboxInfo)

        // then
        assertNotNull(result)
        assertEquals(outboxId, result.outboxId)
        assertEquals(orderId, result.orderId)
        assertEquals(OutboundEventType.RESERVATION_RELEASE_REQUEST, result.eventType)

        val resultPayload = result.payload as ReservationReleasePayloadReservation
        assertEquals(reservationId, resultPayload.reservationId)
    }
}

