package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.OutboundPayload
import nuts.commerce.inventoryservice.event.outbound.ProductEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ProductStockDecrementPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationConfirmSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationFailPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationCreationSuccessPayload
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ReservationReleaseSuccessPayload
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.message.ReservationEventProducer
import nuts.commerce.inventoryservice.port.message.ProduceResult
import nuts.commerce.inventoryservice.port.message.ProductEventProducer
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.Executor

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val reservationEventProducer: ReservationEventProducer,
    private val productEventProducer: ProductEventProducer,
    private val objectMapper: ObjectMapper,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${system.outbox-publisher.claim-batch-size}") private val batchSize: Int,
    @Value($$"${system.outbox-publisher.claim-locked-by}") private val claimLockedBy: String
) {
    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(batchSize, claimLockedBy)
        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.outboxInfo.forEach { outboxInfo ->
            reservationEventProducing(generateReservationOutboundEvent(outboxInfo), outboxUpdateExecutor)
            productEventProducing(generateProductOutboundEvent(outboxInfo), outboxUpdateExecutor)
        }
    }

    private fun reservationEventProducing(
        evnet: ReservationOutboundEvent,
        executor: Executor
    ) {
        reservationEventProducer.produce(evnet)
            .whenCompleteAsync(
                { result, _ ->
                    when (result) {
                        is ProduceResult.Success -> outboxRepository.markPublished(evnet.outboxId, claimLockedBy)
                        is ProduceResult.Failure -> outboxRepository.markFailed(evnet.outboxId, claimLockedBy)
                    }
                },
                executor
            )
    }

    private fun productEventProducing(
        eventList: List<ProductOutboundEvent>,
        executor: Executor
    ) {
        eventList.forEach { productEvent ->
            productEventProducer.produce(productEvent)
                .whenCompleteAsync(
                    { result, _ ->
                        if (result) {
                            println("Successfully produced product event: ${productEvent.eventType} for orderId: ${productEvent.eventId}")
                        } else {
                            println("Failed to produce product event: ${productEvent.eventType} for orderId: ${productEvent.eventId}")
                        }
                    },
                    executor
                )
        }
    }

    private fun generateProductOutboundEvent(outboxInfo: OutboxInfo): List<ProductOutboundEvent> {
        return when (outboxInfo.eventType) {
            OutboundEventType.RESERVATION_CREATION_SUCCEEDED ->
                objectMapper.readValue(outboxInfo.payload, ReservationCreationSuccessPayload::class.java)
                    .reservationItemInfoList
                    .map { reservationItem ->
                        ProductOutboundEvent(
                            eventType = ProductEventType.DECREMENT_STOCK,
                            payload = ProductStockDecrementPayload(
                                orderId = outboxInfo.orderId,
                                productId = reservationItem.productId,
                                qty = reservationItem.qty
                            )
                        )
                    }

            OutboundEventType.RESERVATION_RELEASE ->
                objectMapper.readValue(outboxInfo.payload, ReservationReleaseSuccessPayload::class.java)
                    .reservationItemInfoList
                    .map { reservationItem ->
                        ProductOutboundEvent(
                            eventType = ProductEventType.INCREMENT_STOCK,
                            payload = ProductStockDecrementPayload(
                                orderId = outboxInfo.orderId,
                                productId = reservationItem.productId,
                                qty = reservationItem.qty
                            )
                        )
                    }

            else -> emptyList()
        }
    }

    private fun generateReservationOutboundEvent(outboxInfo: OutboxInfo): ReservationOutboundEvent =
        when (outboxInfo.eventType) {
            OutboundEventType.RESERVATION_CREATION_SUCCEEDED -> createReservationOutboundEvent(
                outboxInfo, ReservationCreationSuccessPayload::class.java
            )

            OutboundEventType.RESERVATION_CREATION_FAILED -> createReservationOutboundEvent(
                outboxInfo, ReservationCreationFailPayload::class.java
            )

            OutboundEventType.RESERVATION_CONFIRM -> createReservationOutboundEvent(
                outboxInfo, ReservationConfirmSuccessPayload::class.java
            )

            OutboundEventType.RESERVATION_RELEASE -> createReservationOutboundEvent(
                outboxInfo, ReservationReleaseSuccessPayload::class.java
            )
        }

    private fun createReservationOutboundEvent(
        outboxInfo: OutboxInfo,
        clazz: Class<out OutboundPayload>
    ): ReservationOutboundEvent {

        return ReservationOutboundEvent(
            orderId = outboxInfo.orderId,
            outboxId = outboxInfo.outboxId,
            eventType = outboxInfo.eventType,
            payload = objectMapper.readValue(
                outboxInfo.payload,
                clazz
            )
        )
    }
}