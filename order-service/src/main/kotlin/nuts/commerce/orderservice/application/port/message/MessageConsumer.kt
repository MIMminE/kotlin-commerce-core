package nuts.commerce.orderservice.application.port.message

import java.time.Instant
import java.util.UUID

interface MessageConsumer {
    fun onPaymentResult(event: PaymentResultEvent)

    data class PaymentResultEvent(
        val eventId: UUID,
        val eventType: String,      // "payment.succeeded" | "payment.failed"
        val orderId: UUID,      // orderId (Kafka key도 이 값으로)
        val payload: String,        // JSON string (필요하면 나중에 강타입으로 교체)
        val occurredAt: Instant? = null,
        val correlationId: UUID? = null // 결제요청 eventId 또는 paymentId 등을 매핑
    )
}