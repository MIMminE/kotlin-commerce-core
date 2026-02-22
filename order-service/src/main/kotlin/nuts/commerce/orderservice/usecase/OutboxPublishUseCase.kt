package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.event.outbound.OrderOutboundEvent
import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.orderservice.port.message.OrderEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import nuts.commerce.orderservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val orderEventProducer: OrderEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${system.outbox-publisher.claim-batch-size}") private val batchSize: Int,
    @Value($$"${system.outbox-publisher.claim-locked-by}") private val claimLockedBy: String,
    orderEventConverterList: List<OutboundEventConverter>
) {
    private val orderEventConverterMap: Map<OutboundEventType, OutboundEventConverter> =
        orderEventConverterList.associateBy { it.supportType }

    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(batchSize, claimLockedBy)
        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.outboxInfo.forEach { outboxInfo ->
            orderEventConverterMap[outboxInfo.eventType]?.let { converter ->
                orderEventProducing(converter.convert(outboxInfo), outboxUpdateExecutor)
            } ?: throw IllegalArgumentException("No converter found for event type: ${outboxInfo.eventType}")


        }
    }

    private fun orderEventProducing(
        event: OrderOutboundEvent,
        executor: Executor
    ) {
        orderEventProducer.produce(event)
            .whenCompleteAsync(
                { result, _ ->
                    when (result) {
                        is ProduceResult.Success -> outboxRepository.markPublished(result.outboxId, claimLockedBy)
                        is ProduceResult.Failure -> outboxRepository.markFailed(result.outboxId, claimLockedBy)
                    }
                }, executor
            )
    }
}