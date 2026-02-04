package nuts.commerce.paymentservice.application.port.message

import java.util.UUID


interface PaymentEventConsumer {
    fun consume(message: InboundMessage)

    data class InboundMessage(
        val eventId: UUID,
        val eventType: String,
        val payload: String,
        val aggregateId: UUID
    )
}