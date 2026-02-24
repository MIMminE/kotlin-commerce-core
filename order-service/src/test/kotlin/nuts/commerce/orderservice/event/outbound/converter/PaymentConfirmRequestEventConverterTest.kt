package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.model.OutboxInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class PaymentConfirmRequestEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var converter: PaymentConfirmRequestEventConverter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        converter = PaymentConfirmRequestEventConverter(objectMapper)
    }

    @Test
    fun `supportType은 PAYMENT_CONFIRM_REQUEST를 반환한다`() {
        // when & then
        assertEquals(OutboundEventType.PAYMENT_CONFIRM_REQUEST, converter.supportType)
    }

    @Test
    fun `OutboxInfo를 PaymentOutboundEvent로 변환한다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()

        val payload = PaymentConfirmPayload(paymentId = paymentId)
        val payloadJson = objectMapper.writeValueAsString(payload)

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CONFIRM_REQUEST,
            payload = payloadJson
        )

        // when
        val result = converter.convert(outboxInfo)

        // then
        assertNotNull(result)
        assertEquals(outboxId, result.outboxId)
        assertEquals(orderId, result.orderId)
        assertEquals(OutboundEventType.PAYMENT_CONFIRM_REQUEST, result.eventType)

        val resultPayload = result.payload as PaymentConfirmPayload
        assertEquals(paymentId, resultPayload.paymentId)
    }
}

