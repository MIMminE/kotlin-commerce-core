package nuts.commerce.orderservice.event.inbound.handler

import nuts.commerce.orderservice.event.inbound.InboundEventType
import nuts.commerce.orderservice.event.inbound.OrderInboundEvent

interface InboundEventHandler {
    val supportType: InboundEventType
    fun handle(orderInboundEvent: OrderInboundEvent)
}