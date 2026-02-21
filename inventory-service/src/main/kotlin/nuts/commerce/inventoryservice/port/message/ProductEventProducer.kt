package nuts.commerce.inventoryservice.port.message

import nuts.commerce.inventoryservice.event.ProductOutboundEvent
import java.util.concurrent.CompletableFuture

interface ProductEventProducer {
    fun produce(outboundEvent: ProductOutboundEvent): CompletableFuture<Boolean>
}
