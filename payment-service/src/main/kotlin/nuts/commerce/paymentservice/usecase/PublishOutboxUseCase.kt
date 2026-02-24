package nuts.commerce.paymentservice.usecase

import nuts.commerce.paymentservice.event.outbound.OutboundEventType
import nuts.commerce.paymentservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.paymentservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.paymentservice.port.message.PaymentEventProducer
import nuts.commerce.paymentservice.port.message.ProduceResult
import nuts.commerce.paymentservice.port.repository.ClaimOutboxResult
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executor

@Component
class PublishOutboxUseCase(
    private val outboxRepository: OutboxRepository,
    private val paymentEventProducer: PaymentEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${system.outbox-publisher.claim-batch-size}") private val batchSize: Int,
    @Value($$"${system.outbox-publisher.claim-locked-by}") private val claimLockedBy: String,
    paymentEventConverterList: List<OutboundEventConverter>
) {

    private val eventConverterMap: Map<OutboundEventType, OutboundEventConverter> =
        paymentEventConverterList.associateBy { it.supportType }

    @Transactional
    fun claim(): ClaimOutboxResult {
        return outboxRepository.claimAndLockBatchIds(batchSize, claimLockedBy)
    }


    fun publish(claimOutboxResult: ClaimOutboxResult) {
        claimOutboxResult.outboxInfo.forEach { outboxInfo ->
            eventConverterMap[outboxInfo.eventType]?.let { converter ->
                paymentEventProducing(converter.convert(outboxInfo))
            } ?: throw IllegalStateException("No converter found for event type: ${outboxInfo.eventType}")
        }
    }

    private fun paymentEventProducing(
        event: PaymentOutboundEvent
    ) {
        paymentEventProducer.produce(event)
            .whenCompleteAsync(
                { result, _ ->
                    when (result) {
                        is ProduceResult.Success ->
                            outboxRepository.markPublished(
                                event.outboxId,
                                claimLockedBy
                            )

                        is ProduceResult.Failure ->
                            outboxRepository.markFailed(
                                event.outboxId,
                                claimLockedBy
                            )

                    }
                }, outboxUpdateExecutor
            )
    }
}