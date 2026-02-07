package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.port.message.QuantityUpdateEvent
import nuts.commerce.inventoryservice.port.message.QuantityUpdateEventProducer
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Component
class KafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, QuantityUpdateEvent>,
    @Value($$"${inventory.kafka.topic.quantity-update}")
    private val topic: String
) : QuantityUpdateEventProducer {

    override fun produce(
        inventoryId: UUID,
        event: QuantityUpdateEvent
    ): CompletableFuture<Unit> {
        return kafkaTemplate.send(topic, inventoryId.toString(), event)
            .thenApply { Unit }
    }
}