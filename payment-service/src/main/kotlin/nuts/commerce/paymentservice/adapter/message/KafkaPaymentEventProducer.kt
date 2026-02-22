package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture

@Component
class KafkaPaymentEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, PaymentOutboundEvent>,
    @Value($$"${kafka.payment-event-producer.topic}")
    private val topic: String,
    private val objectMapper: ObjectMapper
) : PaymentEventProducer {

    override fun produce(paymentOutboundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult> {
        return kafkaTemplate.send(topic, paymentOutboundEvent)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = paymentOutboundEvent.eventId,
                        outboxId = paymentOutboundEvent.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = paymentOutboundEvent.outboxId
                    )
                }
            }
    }
}