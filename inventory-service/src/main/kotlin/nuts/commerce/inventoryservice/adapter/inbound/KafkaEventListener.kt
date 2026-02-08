package nuts.commerce.inventoryservice.adapter.inbound

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import nuts.commerce.inventoryservice.usecase.ReservationCommitUseCase
import nuts.commerce.inventoryservice.usecase.ReservationRequestUseCase
import nuts.commerce.inventoryservice.usecase.ReservationReleaseUseCase
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.core.type.TypeReference
import java.util.UUID

@Component
class KafkaEventListener(
    private val reservationRequestUseCase: ReservationRequestUseCase,
    private val reservationCommitUseCase: ReservationCommitUseCase,
    private val reservationReleaseUseCase: ReservationReleaseUseCase,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${inventory.inbound.topic}"])
    fun onMessage(record: ConsumerRecord<String, String>) {
        val envelope = try {
            objectMapper.readValue(record.value(), EventEnvelope::class.java)
        } catch (ex: Exception) {
            log.warn("Failed to parse event envelope, skipping record: {}", ex.message)
            return
        }

        when (envelope.eventType) {
            "RESERVE_INVENTORY_REQUEST" -> {
                val cmd = try {
                    // parse items array into List<ReservationRequestUseCase.Command.Item>
                    val itemsNode = envelope.payload.get("items") ?: objectMapper.createArrayNode()
                    val items: List<ReservationRequestUseCase.Command.Item> = objectMapper.convertValue(
                        itemsNode,
                        object : TypeReference<List<ReservationRequestUseCase.Command.Item>>() {}
                    )
                    // try to read idempotencyKey if present using textValue()
                    val idempNode = envelope.payload.get("idempotencyKey")
                    val idemp = idempNode?.textValue()?.let { UUID.fromString(it) } ?: UUID.randomUUID()
                    ReservationRequestUseCase.Command(orderId = envelope.aggregateId, idempotencyKey = idemp, items = items)
                } catch (ex: Exception) {
                    log.warn("Failed to parse ReservationRequest payload: {}", ex.message)
                    ReservationRequestUseCase.Command(orderId = envelope.aggregateId, idempotencyKey = UUID.randomUUID(), items = emptyList())
                }
                reservationRequestUseCase.execute(cmd)
            }

            "RESERVE_INVENTORY_CONFIRM" -> {
                // aggregateId expected to be orderId
                reservationCommitUseCase.execute(envelope.aggregateId)
            }

            "RESERVE_INVENTORY_RELEASE" -> {
                // aggregateId expected to be orderId
                reservationReleaseUseCase.execute(envelope.aggregateId)
            }

            else -> {
                log.debug("Ignoring unknown event type: {}", envelope.eventType)
            }
        }
    }
}

data class EventEnvelope(
    val eventType: String,
    val aggregateId: UUID,
    val payload: JsonNode
)