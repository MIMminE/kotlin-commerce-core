package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.event.outbound.OutboundEventType
import nuts.commerce.inventoryservice.event.outbound.ProductOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.inventoryservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.inventoryservice.event.outbound.converter.ProductOutboundEvents
import nuts.commerce.inventoryservice.port.message.ProduceResult
import nuts.commerce.inventoryservice.port.message.ProductEventProducer
import nuts.commerce.inventoryservice.port.message.ReservationEventProducer
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
    @Value($$"${system.outbox-publisher.claim-locked-by}") private val claimLockedBy: String,
    reservationEventConverterList: List<OutboundEventConverter<ReservationOutboundEvent, OutboundEventType>>,
    productEventConverterList: List<OutboundEventConverter<ProductOutboundEvents, OutboundEventType>>
) {

    private val reservationEventConverterMap: Map<OutboundEventType, OutboundEventConverter<ReservationOutboundEvent, OutboundEventType>> =
        reservationEventConverterList.associateBy { it.supportType }

    private val productEventConverterMap: Map<OutboundEventType, OutboundEventConverter<ProductOutboundEvents, OutboundEventType>> =
        productEventConverterList.associateBy { it.supportType }

    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(batchSize, claimLockedBy)
        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.outboxInfo.forEach { outboxInfo ->
            reservationEventConverterMap[outboxInfo.eventType]?.let { converter ->
                reservationEventProducing(converter.convert(outboxInfo), outboxUpdateExecutor)
            } ?: throw IllegalArgumentException("No converter found for event type: ${outboxInfo.eventType}")

            productEventConverterMap[outboxInfo.eventType]?.let { converter ->
                productEventProducing(converter.convert(outboxInfo).items, outboxUpdateExecutor)
            }
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
}