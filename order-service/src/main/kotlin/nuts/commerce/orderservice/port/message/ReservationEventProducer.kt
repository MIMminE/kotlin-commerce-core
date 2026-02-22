package nuts.commerce.orderservice.port.message

import nuts.commerce.orderservice.event.outbound.ReservationOutboundEvent
import java.util.concurrent.CompletableFuture


interface ReservationEventProducer {
    fun produce(outboundEvent: ReservationOutboundEvent): CompletableFuture<ProduceResult>
}
