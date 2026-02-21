package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.event.ProductOutboundEvent
import nuts.commerce.inventoryservice.port.message.ProductEventProducer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class KafkaProductEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ProductOutboundEvent>,
    @Value($$"${kafka.product-event-producer.topic}") private val topic: String
) : ProductEventProducer {

    private val logger = LoggerFactory.getLogger(KafkaProductEventProducer::class.java)

    override fun produce(outboundEvent: ProductOutboundEvent): CompletableFuture<Boolean> =
        kafkaTemplate.send(topic, outboundEvent)
            .handle { _, ex ->
                if (ex != null) {
                    logger.error("Failed to send product event: ${ex.message}", ex)
                    false
                } else {
                    true
                }
            }
}