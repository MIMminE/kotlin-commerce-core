package nuts.commerce.inventoryservice.adapter.inbound

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import nuts.commerce.inventoryservice.usecase.ReservationClaimUseCase
import nuts.commerce.inventoryservice.usecase.ReservationCommitUseCase
import nuts.commerce.inventoryservice.usecase.ReservationRequestUseCase
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class KafkaEventListener(
    private val reservationClaimUseCase: ReservationClaimUseCase,
    private val reservationRequestUseCase: ReservationRequestUseCase,
    private val reservationCommitUseCase: ReservationCommitUseCase,
    private val reservationCancelUseCase: ReservationCommitUseCase,
    private val objectMapper: ObjectMapper
) {

    @KafkaListener(topics = [$$"${inventory.inbound.topic}"])
    fun onMessage(record: ConsumerRecord<String, String>) {
        val envelope = objectMapper.readValue(record.value(), EventEnvelope::class.java)

        when (envelope.eventType) {
            "RESERVE_INVENTORY_REQUEST" -> {
                val cmd = try {
                    objectMapper.convertValue(envelope.payload, ReservationRequestUseCase.Command::class.java)
                } catch (_: Exception) {
                    ReservationRequestUseCase.Command(orderId = envelope.aggregateId, idempotencyKey = UUID.randomUUID(), items = emptyList())
                }
                reservationRequestUseCase.execute(cmd)
            }

            "RESERVE_INVENTORY_CONFIRM" -> reservationClaimUseCase.execute(envelope.aggregateId)

            "RESERVE_INVENTORY_RELEASE" -> reservationCommitUseCase.execute(envelope.aggregateId)
            else -> {

            }
        }
    }
}

data class EventEnvelope(
    val eventType: String,
    val aggregateId: UUID,
    val payload: JsonNode
)