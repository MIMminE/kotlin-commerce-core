package nuts.commerce.paymentservice.adapter.message

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.OutboundPayload
import nuts.commerce.paymentservice.event.outbound.PaymentCreationSuccessPayload
import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture

@Component
class KafkaEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, PaymentOutboundEvent>,
    @Value($$"${payment.kafka.outbound.topic:payment-outbound}")
    private val paymentTopic: String,
    private val objectMapper: ObjectMapper
) : PaymentEventProducer {

    override fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult> {

        val event = when (outboxInfo.eventType) {
            OutboundEventType.PAYMENT_CREATION_SUCCEEDED -> createPaymentOutboundEvent(
                outboxInfo,
                PaymentCreationSuccessPayload::class.java
            )

            OutboundEventType.PAYMENT_CREATION_FAILED -> createPaymentOutboundEvent(
                outboxInfo,
                PaymentCreationSuccessPayload::class.java
            )

            OutboundEventType.PAYMENT_CONFIRM -> createPaymentOutboundEvent(
                outboxInfo,
                PaymentCreationSuccessPayload::class.java
            )

            OutboundEventType.PAYMENT_RELEASE -> createPaymentOutboundEvent(
                outboxInfo,
                PaymentCreationSuccessPayload::class.java
            )
        }

        return kafkaTemplate.send(paymentTopic, event)
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

    private fun createPaymentOutboundEvent(
        outboxInfo: OutboxInfo,
        clazz: Class<out OutboundPayload>
    ): PaymentOutboundEvent {

        return PaymentOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            paymentId = outboxInfo.paymentId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                clazz
            )
        )
    }
}