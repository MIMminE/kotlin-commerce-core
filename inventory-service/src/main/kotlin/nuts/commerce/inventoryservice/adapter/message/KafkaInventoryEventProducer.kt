package nuts.commerce.inventoryservice.adapter.message

import nuts.commerce.inventoryservice.event.OutboundEventType.*
import nuts.commerce.inventoryservice.event.OutboundPayload
import nuts.commerce.inventoryservice.event.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.InventoryEventProducer
import nuts.commerce.inventoryservice.port.message.ProduceResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CompletableFuture

@Component
class KafkaInventoryEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ReservationOutboundEvent>,
    @Value($$"${inventory.kafka.outbound.topic:inventory-outbound}")
    private val inventoryTopic: String,
    private val objectMapper: ObjectMapper
) : InventoryEventProducer {

    override fun produce(outboxInfo: OutboxInfo): CompletableFuture<ProduceResult> {

        val event =
            when (outboxInfo.eventType) {
                RESERVATION_CREATION_SUCCEEDED -> createReservationOutboundEvent(
                    outboxInfo,
                    ReservationCreationSuccessPayload::class.java
                )

                RESERVATION_CREATION_FAILED -> createReservationOutboundEvent(
                    outboxInfo,
                    ReservationCreationFailPayload::class.java
                )

                RESERVATION_CONFIRM -> createReservationOutboundEvent(
                    outboxInfo,
                    ReservationConfirmSuccessPayload::class.java
                )

                RESERVATION_RELEASE -> createReservationOutboundEvent(
                    outboxInfo,
                    ReservationReleaseSuccessPayload::class.java
                )
            }

        return kafkaTemplate.send(inventoryTopic, event)
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

    private fun createReservationOutboundEvent(
        outboxInfo: OutboxInfo,
        clazz: Class<out OutboundPayload>
    ): ReservationOutboundEvent {

        return ReservationOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            reservationId = outboxInfo.reservationId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                clazz
            )
        )
    }
}