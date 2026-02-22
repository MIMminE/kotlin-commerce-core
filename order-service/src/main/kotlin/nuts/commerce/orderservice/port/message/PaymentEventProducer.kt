package nuts.commerce.orderservice.port.message

import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import java.util.concurrent.CompletableFuture

interface PaymentEventProducer {
    fun produce(paymentOutBoundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult>
}