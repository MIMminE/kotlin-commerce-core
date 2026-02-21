package nuts.commerce.inventoryservice.adapter.system

import jakarta.annotation.PostConstruct
import nuts.commerce.inventoryservice.event.InboundEventType
import nuts.commerce.inventoryservice.event.ReservationInboundEvent
import nuts.commerce.inventoryservice.usecase.ReservationConfirmCommand
import nuts.commerce.inventoryservice.usecase.ReservationConfirmUseCase
import nuts.commerce.inventoryservice.usecase.ReservationReleaseCommand
import nuts.commerce.inventoryservice.usecase.ReservationReleaseUseCase
import nuts.commerce.inventoryservice.usecase.ReservationCreateCommand
import nuts.commerce.inventoryservice.usecase.ReservationCreateUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@ConditionalOnProperty(
    prefix = "system.reservation-event-listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class ReservationEventListener(
    private val reservationCreateUseCase: ReservationCreateUseCase,
    private val reservationConfirmUseCase: ReservationConfirmUseCase,
    private val reservationReleaseUseCase: ReservationReleaseUseCase
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        log.info("KafkaEventListener initialized")
    }

    @KafkaListener(
        topics = [$$"${system.reservation-event-listener.topic}"],
        groupId = $$"${system.reservation-event-listener.group-id}",
    )
    fun onMessage(
        @Payload inboundEvent: ReservationInboundEvent
    ) {
        when (inboundEvent.eventType) {
            InboundEventType.RESERVATION_CREATE -> reservationCreateUseCase.execute(
                ReservationCreateCommand.from(
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