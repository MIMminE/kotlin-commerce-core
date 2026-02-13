package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.port.message.PaymentEvent
import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture


@ConditionalOnProperty(
    prefix = "payment.kafka.producer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventProducer(
    private val eventMapperRegistry: EventMapperRegistry,
    private val kafkaTemplate: KafkaTemplate<String, PaymentEvent>,
    @Value($$"${payment.kafka.producer.topic:payment-outbound}")
    private val paymentTopic: String
) : PaymentEventProducer {

    override fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult> {
        val event = eventMapperRegistry.map(outboxInfo)
        return kafkaTemplate.send(paymentTopic, event)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = event.eventId,
                        outboxId = outboxInfo.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = outboxInfo.outboxId
                    )
                }
            }
    }
}