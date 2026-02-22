package nuts.commerce.orderservice.usecase

import nuts.commerce.orderservice.event.outbound.OutboundEventType
import nuts.commerce.orderservice.event.outbound.PaymentOutboundEvent
import nuts.commerce.orderservice.event.outbound.ReservationOutboundEvent
import nuts.commerce.orderservice.event.outbound.converter.OutboundEventConverter
import nuts.commerce.orderservice.port.message.ReservationEventProducer
import nuts.commerce.orderservice.port.message.PaymentEventProducer
import nuts.commerce.orderservice.port.message.ProduceResult
import nuts.commerce.orderservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val reservationEventProducer: ReservationEventProducer,
    private val paymentEventProducer: PaymentEventProducer,
    @Qualifier("outboxUpdateExecutor") private val outboxUpdateExecutor: Executor,
    @Value($$"${system.outbox-publisher.claim-batch-size}") private val batchSize: Int,
    @Value($$"${system.outbox-publisher.claim-locked-by}") private val claimLockedBy: String,
    reservationOutboundEventConverters: List<OutboundEventConverter<ReservationOutboundEvent>>,
    paymentOutboundEventConverters: List<OutboundEventConverter<PaymentOutboundEvent>>
) {
    private val reservationEventConverterMap: Map<OutboundEventType, OutboundEventConverter<ReservationOutboundEvent>> =
        reservationOutboundEventConverters.associateBy { it.supportType }

    private val paymentEventConverterMap: Map<OutboundEventType, OutboundEventConverter<PaymentOutboundEvent>> =
        paymentOutboundEventConverters.associateBy { it.supportType }


    fun execute() {
        val claimedOutboxResults = outboxRepository.claimAndLockBatchIds(batchSize, claimLockedBy)
        if (claimedOutboxResults.size == 0) {
            return
        }

        claimedOutboxResults.outboxInfo.forEach { outboxInfo ->
            when (outboxInfo.eventType.eventClass) {
                ReservationOutboundEvent::class.java ->
                    reservationEventConverterMap[outboxInfo.eventType]?.let { converter ->
                        reservationEventProducing(converter.convert(outboxInfo))
                    } ?: throw IllegalArgumentException("No converter found for event type: ${outboxInfo.eventType}")

                PaymentOutboundEvent::class.java ->
                    paymentEventConverterMap[outboxInfo.eventType]?.let { converter ->
                        paymentEventProducing(converter.convert(outboxInfo))
                    } ?: throw IllegalArgumentException("No converter found for event type: ${outboxInfo.eventType}")

                else -> throw IllegalArgumentException("Unsupported event class: ${outboxInfo.eventType.eventClass}")
            }
        }
    }


    private fun reservationEventProducing(event: ReservationOutboundEvent) {
        reservationEventProducer.produce(event)
            .whenCompleteAsync(
                { result, _ ->
                    when (result) {
                        is ProduceResult.Success -> outboxRepository.markPublished(result.outboxId, claimLockedBy)
                        is ProduceResult.Failure -> outboxRepository.markFailed(result.outboxId, claimLockedBy)
                    }
                }, outboxUpdateExecutor
            )
    }

    private fun paymentEventProducing(event: PaymentOutboundEvent) {
        paymentEventProducer.produce(event)
            .whenCompleteAsync(
                { result, _ ->
                    when (result) {
                        is ProduceResult.Success -> outboxRepository.markPublished(result.outboxId, claimLockedBy)
                        is ProduceResult.Failure -> outboxRepository.markFailed(result.outboxId, claimLockedBy)
                    }
                }, outboxUpdateExecutor
            )
    }
}