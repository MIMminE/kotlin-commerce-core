package nuts.commerce.paymentservice.port.message

import nuts.commerce.paymentservice.model.EventType
import nuts.commerce.paymentservice.model.OutboxInfo

interface PaymentEventMapper {
    val eventType: EventType
    fun map(outboxInfo: OutboxInfo): PaymentEvent
}