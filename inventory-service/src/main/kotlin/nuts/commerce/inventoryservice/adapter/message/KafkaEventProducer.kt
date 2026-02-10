package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.InventoryEvent
import nuts.commerce.inventoryservice.port.message.ProduceResult
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@ConditionalOnProperty(
    name = ["kafka.producer.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
@EnableConfigurationProperties(InventoryTopicProperties::class)
class KafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, InventoryEvent>,
    private val props: InventoryTopicProperties
) : InventoryEventProducer {

    override fun produce(inventoryEvent: InventoryEvent): CompletableFuture<ProduceResult> {
        val topic = when (inventoryEvent.eventType) {
            EventType.RESERVATION_CREATION -> props.reservationCreation
            EventType.RESERVATION_COMMITTED -> props.reservationCommitted
            EventType.RESERVATION_RELEASED -> props.reservationReleased
            else -> {
                throw IllegalArgumentException("Unsupported event type: ${inventoryEvent.eventType}")
            }
        }

        return kafkaTemplate.send(topic, inventoryEvent.orderId.toString(), inventoryEvent)
            .handle { _, ex ->
                return@handle when (ex) {
                    null -> ProduceResult.Success(
                        eventId = inventoryEvent.eventId,
                        outboxId = inventoryEvent.outboxId
                    )

                    else -> ProduceResult.Failure(
                        reason = ex.message ?: "Unknown error",
                        outboxId = inventoryEvent.outboxId
                    )
                }
            }
    }
}

@ConfigurationProperties(prefix = "inventory.kafka.topics")
data class InventoryTopicProperties(
    var reservationCreation: String = "reservation-creations",
    var reservationCommitted: String = "reservation-commits",
    var reservationReleased: String = "reservation-releases",
)