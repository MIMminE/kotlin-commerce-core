package nuts.commerce.paymentservice.event.outbound.converter

import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.model.OutboxInfo

interface OutboundEventConverter {
    val supportType: OutboundEventType
    fun convert(outboxInfo: OutboxInfo): PaymentOutboundEvent
}