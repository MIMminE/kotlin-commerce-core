package nuts.commerce.orderservice.application.port.message

import java.time.Instant
import java.util.UUID

interface MessageConsumer {
    fun onPaymentResult(message: PaymentResultMessage)

    data class PaymentResultMessage(
        val eventId: UUID,
        val eventType: String,
        val orderId: UUID,
        val payload: String,
        val occurredAt: Instant? = null,
        val correlationId: UUID? = null
    )
}