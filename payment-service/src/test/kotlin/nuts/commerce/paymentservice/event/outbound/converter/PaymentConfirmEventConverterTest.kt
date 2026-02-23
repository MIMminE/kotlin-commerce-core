package nuts.commerce.paymentservice.event.outbound.converter

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentConfirmSuccessPayload
import nuts.commerce.paymentservice.model.OutboxInfo
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@Suppress("NonAsciiCharacters")
class PaymentConfirmEventConverterTest {

    private val objectMapper = ObjectMapper()
    private val converter = PaymentConfirmEventConverter(objectMapper)

    @Test
    fun `컨버터 타입은 PAYMENT_CONFIRM 이다`() {
        assertEquals(OutboundEventType.PAYMENT_CONFIRM, converter.supportType)
    }

    @Test
    fun `아웃박스 정보를 PaymentOutboundEvent로 변환한다`() {
        val outboxId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val paymentId = UUID.randomUUID()
        val payload = PaymentConfirmSuccessPayload(
            paymentProvider = "InMemoryPaymentProvider",
            providerPaymentId = UUID.randomUUID()
        )

        val info = OutboxInfo(
            outboxId = outboxId,
            orderId = orderId,
            paymentId = paymentId,
            eventType = OutboundEventType.PAYMENT_CONFIRM,
            payload = objectMapper.writeValueAsString(payload)
        )

        val event = converter.convert(info)

        assertEquals(outboxId, event.outboxId)
        assertEquals(orderId, event.orderId)
        assertEquals(paymentId, event.paymentId)
        assertEquals(OutboundEventType.PAYMENT_CONFIRM, event.eventType)
        assertIs<PaymentConfirmSuccessPayload>(event.payload)
        val converted = event.payload as PaymentConfirmSuccessPayload
        assertEquals(payload.paymentProvider, converted.paymentProvider)
        assertEquals(payload.providerPaymentId, converted.providerPaymentId)
    }
}

