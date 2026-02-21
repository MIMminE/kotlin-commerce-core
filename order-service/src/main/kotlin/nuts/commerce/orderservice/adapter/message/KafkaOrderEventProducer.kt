package nuts.commerce.orderservice.adapter.message

import nuts.commerce.orderservice.event.outbound.OrderOutboundEvent
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.OutboundPayload
import nuts.commerce.orderservice.event.outbound.PaymentConfirmPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreateFailedPayload
import nuts.commerce.orderservice.event.outbound.PaymentCreatePayload
import nuts.commerce.orderservice.event.outbound.PaymentReleasePayload
import nuts.commerce.orderservice.event.outbound.ReservationConfirmPayload
import nuts.commerce.orderservice.event.outbound.ReservationCreatePayload
import nuts.commerce.orderservice.event.outbound.ReservationReleasePayload
import nuts.commerce.orderservice.model.OutboxInfo
import nuts.commerce.orderservice.port.message.OrderEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture

@Component
class KafkaOrderEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, OrderOutboundEvent>,
    @Value($$"${order.kafka.outbound.topic:order-outbound}")
    private val orderTopic: String,
    private val objectMapper: ObjectMapper

) : OrderEventProducer {

    override fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult> {
        val event =
            when (outboxInfo.eventType) {
                OutboundEventType.RESERVATION_CREATE_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    ReservationCreatePayload::class.java
                )

                OutboundEventType.RESERVATION_CONFIRM_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    ReservationConfirmPayload::class.java
                )

                OutboundEventType.RESERVATION_RELEASE_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    ReservationReleasePayload::class.java
                )

                OutboundEventType.PAYMENT_CREATE_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    PaymentCreatePayload::class.java
                )

                OutboundEventType.PAYMENT_CREATE_FAILED -> createOrderOutboundEvent(
                    outboxInfo,
                    PaymentCreateFailedPayload::class.java
                )

                OutboundEventType.PAYMENT_CONFIRM_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    PaymentConfirmPayload::class.java
                )

                OutboundEventType.PAYMENT_RELEASE_REQUEST -> createOrderOutboundEvent(
                    outboxInfo,
                    PaymentReleasePayload::class.java
                )
            }

        return kafkaTemplate.send(orderTopic, event)
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

    private fun createOrderOutboundEvent(
        outboxInfo: OutboxInfo,
        payloadClass: Class<out OutboundPayload>
    ): OrderOutboundEvent {

        return OrderOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(outboxInfo.payload, payloadClass)
        )
    }
}