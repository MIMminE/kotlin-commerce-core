package nuts.commerce.paymentservice.adapter.inbound

import nuts.commerce.paymentservice.usecase.PaymentConfirmedUseCase
import nuts.commerce.paymentservice.usecase.PaymentReleasedUseCase
import nuts.commerce.paymentservice.usecase.PaymentRequestedUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import java.util.UUID

@ConditionalOnProperty(
    prefix = "payment.outbox.listener",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@Component
class KafkaEventListener(
    private val paymentRequestUseCase: PaymentRequestedUseCase,
    private val paymentConfirmedUseCase: PaymentConfirmedUseCase,
    private val paymentReleasedUseCase: PaymentReleasedUseCase
) {

    @KafkaListener(topics = ["payment-inbound"], groupId = "payment-service-group")
    fun onMessage(record: ConsumerRecord<String, String>) {
        record.value()

    }
}

data class PaymentEventEnvelope(
    val orderId: UUID,
    val paymentId: UUID,
    val eventId: UUID,
    val eventType: String,
    val payload: JsonNode
)