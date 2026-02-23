package nuts.commerce.paymentservice.event.outbound.converter

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentReleaseSuccessPayload
import nuts.commerce.paymentservice.model.OutboxInfo
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("NonAsciiCharacters")
class PaymentReleaseEventConverterTest {

    private val objectMapper = ObjectMapper()
    private val converter = PaymentReleaseEventConverter(objectMapper)

    @Test
    fun `컨버터 타입은 PAYMENT_RELEASE 이다`() {
        assertEquals(OutboundEventType.PAYMENT_RELEASE, converter.supportType)
    }

    @Test
    fun `아웃박스 정보를 PaymentOutboundEvent로 변환한다`() {
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val payload = PaymentReleaseSuccessPayload(
            paymentProvider = "InMemoryPaymentProvider",
            providerPaymentId = UUID.randomUUID()
        )

        val info = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            paymentId = paymentId,
            eventType = OutboundEventType.PAYMENT_RELEASE,
            payload = objectMapper.writeValueAsString(payload)
        )

        val event = converter.convert(info)

        assertEquals(outboxId, event.outboxId)
        assertEquals(orderId, event.orderId)
        assertEquals(paymentId, event.paymentId)
        assertEquals(OutboundEventType.PAYMENT_RELEASE, event.eventType)
        assertIs<PaymentReleaseSuccessPayload>(event.payload)
        val converted = event.payload as PaymentReleaseSuccessPayload
        assertEquals(payload.paymentProvider, converted.paymentProvider)
        assertEquals(payload.providerPaymentId, converted.providerPaymentId)
    }
}

