package nuts.commerce.orderservice.event.outbound.converter

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
import nuts.commerce.orderservice.model.OutboxInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Suppress("NonAsciiCharacters")
class PaymentCreateRequestEventConverterTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var converter: PaymentCreateRequestEventConverter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        converter = PaymentCreateRequestEventConverter(objectMapper)
    }

    @Test
    fun `supportType은 PAYMENT_CREATE_REQUEST를 반환한다`() {
        // when & then
        assertEquals(OutboundEventType.PAYMENT_CREATE_REQUEST, converter.supportType)
    }

    @Test
    fun `OutboxInfo를 PaymentOutboundEvent로 변환한다`() {
        // given
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val amount = 10000L
        val currency = "KRW"

        val payload = PaymentCreatePayload(amount = amount, currency = currency)
        val payloadJson = objectMapper.writeValueAsString(payload)

        val outboxInfo = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            eventType = OutboundEventType.PAYMENT_CREATE_REQUEST,
            payload = payloadJson
        )

        // when
        val result = converter.convert(outboxInfo)

        // then
        assertNotNull(result)
        assertEquals(outboxId, result.outboxId)
        assertEquals(orderId, result.orderId)
        assertEquals(OutboundEventType.PAYMENT_CREATE_REQUEST, result.eventType)

        val resultPayload = result.payload as PaymentCreatePayload
        assertEquals(amount, resultPayload.amount)
        assertEquals(currency, resultPayload.currency)
    }
}

