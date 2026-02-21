package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.port.message.ReservationEventProducer
import nuts.commerce.inventoryservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaReservationEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ReservationOutboundEvent>,
    @Value($$"${kafka.reservation-event-producer.topic}") private val topic: String,
) : ReservationEventProducer {

    override fun produce(outboundEvent: ReservationOutboundEvent): CompletableFuture<ProduceResult> {
        return kafkaTemplate.send(topic, outboundEvent)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = outboundEvent.eventId,
                        outboxId = outboundEvent.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = outboundEvent.outboxId
                    )
                }
            }
    }
}