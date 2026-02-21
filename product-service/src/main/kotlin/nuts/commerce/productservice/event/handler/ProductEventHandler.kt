package nuts.commerce.productservice.event.handler

import nuts.commerce.productservice.event.InboundEventType
import nuts.commerce.productservice.event.ProductInboundEvent

interface ProductEventHandler {
    val supportType: InboundEventType
    fun handle(productInboundEvent: ProductInboundEvent)
}