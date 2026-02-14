package nuts.commerce.inventoryservice.adapter.inbound

import jakarta.annotation.PostConstruct
import nuts.commerce.inventoryservice.usecase.ReservationConfirmCommand
import nuts.commerce.inventoryservice.usecase.ReservationConfirmUseCase
import nuts.commerce.inventoryservice.usecase.ReservationReleaseCommand
import nuts.commerce.inventoryservice.usecase.ReservationReleaseUseCase
import nuts.commerce.inventoryservice.usecase.ReservationRequestCommand
import nuts.commerce.inventoryservice.usecase.ReservationRequestUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@ConditionalOnProperty(
    prefix = "inventory.outbox.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val reservationRequestUseCase: ReservationRequestUseCase,
    private val reservationConfirmUseCase: ReservationConfirmUseCase,
    private val reservationReleaseUseCase: ReservationReleaseUseCase,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("KafkaEventListener initialized")
    }

    @KafkaListener(topics = ["\${inventory.inbound.topic}"])
    fun onMessage(
        @Payload envelope: KafkaEventEnvelope,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String
    ) {
        when (envelope.eventType) {
            ListenEventType.RESERVATION_REQUEST -> handleReservationRequest(envelope)
            ListenEventType.RESERVATION_CONFIRM -> handleReservationConfirm(envelope)
            ListenEventType.RESERVATION_RELEASE -> handleReservationRelease(envelope)
        }
    }

    private fun handleReservationRequest(envelope: KafkaEventEnvelope) {
        val payload = objectMapper.treeToValue(envelope.payload, RequestPayload::class.java)
        val command = ReservationRequestCommand(
            eventId = envelope.eventId,
            orderId = envelope.orderId,
            items = payload.items.map { item ->
                ReservationRequestCommand.Item(
                    productId = item.productId,
                    qty = item.qty
                )
            }
        )
        reservationRequestUseCase.execute(command)
    }

    private fun handleReservationConfirm(envelope: KafkaEventEnvelope) {
        val payload = objectMapper.treeToValue(envelope.payload, CommitPayload::class.java)
        val command = ReservationConfirmCommand(
            eventId = envelope.eventId,
            orderId = envelope.orderId,
            reservationId = payload.reservationId
        )
        reservationConfirmUseCase.execute(command)
    }

    private fun handleReservationRelease(envelope: KafkaEventEnvelope) {
        val payload = objectMapper.treeToValue(envelope.payload, ReleasePayload::class.java)
        val command = ReservationReleaseCommand(
            eventId = envelope.eventId,
            orderId = envelope.orderId,
            reservationId = payload.reservationId
        )
        reservationReleaseUseCase.execute(command)
    }
}