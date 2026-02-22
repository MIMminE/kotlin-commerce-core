package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.orderservice.port.message.PaymentEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaPaymentEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, PaymentOutboundEvent>,
    @Value($$"${kafka.payment-event-producer.topic}") private val topic: String,
) : PaymentEventProducer {

    override fun produce(paymentOutBoundEvent: PaymentOutboundEvent): CompletableFuture<ProduceResult> {
        return kafkaTemplate.send(topic, paymentOutBoundEvent)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = paymentOutBoundEvent.eventId,
                        outboxId = paymentOutBoundEvent.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = paymentOutBoundEvent.outboxId
                    )
                }
            }
    }
}