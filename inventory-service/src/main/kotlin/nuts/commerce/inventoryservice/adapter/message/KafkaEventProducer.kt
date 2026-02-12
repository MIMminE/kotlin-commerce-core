package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@ConditionalOnProperty(
    prefix = "inventory.kafka.producer",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventProducer(
    private val eventMapperRegistry: EventMapperRegistry,
    private val kafkaTemplate: KafkaTemplate<String, InventoryEvent>,
    @Value($$"${inventory.kafka.producer.inventory-topic}")
    private val inventoryTopic: String
) : InventoryEventProducer {

    override fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult> {
        val event = eventMapperRegistry.map(outboxInfo)

        return kafkaTemplate.send(inventoryTopic, event)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = event.eventId,
                        outboxId = event.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = event.outboxId
                    )
                }
            }
    }
}