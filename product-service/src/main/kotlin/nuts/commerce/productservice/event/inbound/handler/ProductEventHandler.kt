package nuts.commerce.productservice.event.inbound.handler

import nuts.commerce.productservice.event.inbound.InboundEventType
import nuts.commerce.productservice.event.inbound.ProductInboundEvent

interface ProductEventHandler {
    val supportType: InboundEventType
    fun handle(productInboundEvent: ProductInboundEvent)
}