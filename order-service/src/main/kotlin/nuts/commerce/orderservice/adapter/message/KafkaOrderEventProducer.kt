package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.event.outbound.OrderOutboundEvent
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundPayload
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreateFailedPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
import nuts.commerce.orderservice.event.outbound.PaymentReleasePayload
import nuts.commerce.orderservice.event.outbound.ReservationConfirmPayload
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayload
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayload
import nuts.commerce.orderservice.model.OutboxInfo
import nuts.commerce.orderservice.port.message.OrderEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture

@Component
class KafkaOrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, OrderOutboundEvent>,
    @Value($$"${kafka.order-event-producer.topic}")
    private val orderTopic: String,
) : OrderEventProducer {

    override fun produce(outboundEvent: OrderOutboundEvent): CompletableFuture<ProduceResult> {
        return kafkaTemplate.send(orderTopic, outboundEvent)
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