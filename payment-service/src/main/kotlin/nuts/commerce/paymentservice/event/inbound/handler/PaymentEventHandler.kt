package nuts.commerce.paymentservice.event.inbound.handler

import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent

interface PaymentEventHandler {
    val supportType: InboundEventType
    fun handle(paymentInboundEvent: PaymentInboundEvent)
}