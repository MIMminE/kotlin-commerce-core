package nuts.commerce.paymentservice.adapter.inbound

import nuts.commerce.paymentservice.event.inbound.InboundEventType
import nuts.commerce.paymentservice.event.inbound.PaymentInboundEvent
import nuts.commerce.paymentservice.usecase.PaymentConfirmCommand
import nuts.commerce.paymentservice.usecase.PaymentConfirmedUseCase
import nuts.commerce.paymentservice.usecase.PaymentCreateCommand
import nuts.commerce.paymentservice.usecase.PaymentReleasedUseCase
import nuts.commerce.paymentservice.usecase.PaymentCreatedUseCase
import nuts.commerce.paymentservice.usecase.PaymentReleaseCommand
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.util.UUID

@ConditionalOnProperty(
    prefix = "payment.kafka.inbound.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val paymentCreateUseCase: PaymentCreatedUseCase,
    private val paymentConfirmedUseCase: PaymentConfirmedUseCase,
    private val paymentReleasedUseCase: PaymentReleasedUseCase
) {

    @KafkaListener(
        topics = [$$"${payment.kafka.inbound.topic:payment-inbound}"],
        groupId = $$"${payment.kafka.inbound.group-id:payment-service-group}"
    )
    fun onMessage(
        @Payload inboundEvent: PaymentInboundEvent
    ) {
        when (inboundEvent.eventType) {
            InboundEventType.PAYMENT_CREATE -> paymentCreateUseCase.execute(
                PaymentCreateCommand.from(
                    inboundEvent
                )
            )

            InboundEventType.PAYMENT_CONFIRM -> paymentConfirmedUseCase.execute(
                PaymentConfirmCommand.from(
                    inboundEvent
                )
            )

            InboundEventType.PAYMENT_RELEASE -> paymentReleasedUseCase.execute(
                PaymentReleaseCommand.from(
                    inboundEvent
                )
            )
        }
    }
}

data class PaymentEventEnvelope(
    val orderId: UUID,
    val paymentId: UUID,
    val eventId: UUID,
    val eventType: String,
    val payload: JsonNode
)