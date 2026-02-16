package nuts.commerce.inventoryservice.adapter.inbound

import jakarta.annotation.PostConstruct
import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
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
    prefix = "inventory.kafka.listener",
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

    @KafkaListener(topics = [$$"${inventory.inbound.topic}"])
    fun onMessage(
        @Payload inboundEvent: ReservationInboundEvent,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String
    ) {
        when (inboundEvent.eventType) {
            InboundEventType.RESERVATION_REQUEST -> reservationRequestUseCase.execute(
                ReservationRequestCommand.from(
                    inboundEvent
                )
            )

            InboundEventType.RESERVATION_CONFIRM -> reservationConfirmUseCase.execute(
                ReservationConfirmCommand.from(
                    inboundEvent
                )
            )

            InboundEventType.RESERVATION_RELEASE -> reservationReleaseUseCase.execute(
                ReservationReleaseCommand.from(
                    inboundEvent
                )
            )
        }
    }
}