package nuts.commerce.inventoryservice.adapter.inbound

import nuts.commerce.inventoryservice.event.EventEnvelope
import nuts.commerce.inventoryservice.event.ReservationCommitPayload
import nuts.commerce.inventoryservice.event.ReservationReleasePayload
import nuts.commerce.inventoryservice.event.ReservationRequestPayload
import nuts.commerce.inventoryservice.usecase.ReservationClaimUseCase
import nuts.commerce.inventoryservice.usecase.ReservationCommitCommand
import nuts.commerce.inventoryservice.usecase.ReservationCommitUseCase
import nuts.commerce.inventoryservice.usecase.ReservationReleaseCommand
import nuts.commerce.inventoryservice.usecase.ReservationReleaseUseCase
import nuts.commerce.inventoryservice.usecase.ReservationRequestCommand
import nuts.commerce.inventoryservice.usecase.ReservationRequestUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class KafkaEventListener(
    private val reservationClaimUseCase: ReservationClaimUseCase,
    private val reservationRequestUseCase: ReservationRequestUseCase,
    private val reservationCommitUseCase: ReservationCommitUseCase,
    private val reservationReleaseUseCase: ReservationReleaseUseCase,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["\${inventory.inbound.topic}"])
    fun onMessage(record: ConsumerRecord<String, String>) {
        log.info("Received message: key=${record.key()}, partition=${record.partition()}, offset=${record.offset()}")
        val envelope = objectMapper.readValue(record.value(), EventEnvelope::class.java)

        when (envelope.eventType) {
            "ReservationRequest" -> {
                val payload = objectMapper.treeToValue(envelope.payload, ReservationRequestPayload::class.java)
                val requestCommand = ReservationRequestCommand(
                    orderId = envelope.orderId,
                    idempotencyKey = envelope.eventId,
                    items = payload.items.map { ReservationRequestCommand.Item(it.productId, it.qty) }
                )
                reservationRequestUseCase.execute(requestCommand)
            }

            "ReservationCommit" -> {
                val payload = objectMapper.treeToValue(envelope.payload, ReservationCommitPayload::class.java)
                reservationClaimUseCase.execute(payload.reservationId)
                val commitCommand = ReservationCommitCommand(reservationId = payload.reservationId)
                reservationCommitUseCase.execute(commitCommand)
            }

            "ReservationRelease" -> {
                val payload = objectMapper.treeToValue(envelope.payload, ReservationReleasePayload::class.java)
                reservationClaimUseCase.execute(payload.reservationId)

                reservationReleaseUseCase.execute(ReservationReleaseCommand(reservationId = payload.reservationId))
            }

            else -> {
                log.warn("Unknown event type: ${envelope.eventType}")
            }
        }


    }
}