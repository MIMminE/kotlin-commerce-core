package nuts.commerce.orderservice.port.message

import nuts.commerce.orderservice.event.outbound.OrderOutboundEvent
import java.util.UUID
import java.util.concurrent.CompletableFuture


interface OrderEventProducer {
    fun produce(outboundEvent: OrderOutboundEvent): CompletableFuture<ProduceResult>
}

sealed interface ProduceResult {
    data class Success(val eventId: UUID, val outboxId: UUID) : ProduceResult
    data class Failure(val reason: String, val outboxId: UUID) : ProduceResult
}