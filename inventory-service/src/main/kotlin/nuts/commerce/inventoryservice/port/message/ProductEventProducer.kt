package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import java.util.concurrent.CompletableFuture

interface ProductEventProducer {
    fun produce(outboundEvent: ProductOutboundEvent): CompletableFuture<Boolean>
}
