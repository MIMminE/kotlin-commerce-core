package nuts.commerce.inventoryservice.usecase

import nuts.commerce.inventoryservice.port.message.QuantityUpdateEvent
import nuts.commerce.inventoryservice.port.message.QuantityUpdateEventProducer
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class OutboxPublishUseCase(
    private val outboxRepository: OutboxRepository,
    private val producer: QuantityUpdateEventProducer,
    private val objectMapper: ObjectMapper,
    @Value("\${inventory.outbox.batch-size:50}")
    private val batchSize: Int
) {
    fun execute(claimOutboxIdList : List<UUID>) {
        val ids = outboxRepository.claimPendingOutboxRecords(batchSize)
        if (ids.isEmpty()) return

        val records = outboxRepository.getOutboxRecordsListByIds(ids)

        records.forEach { rec ->
            try {
                val event = objectMapper.readValue(rec.payload, QuantityUpdateEvent::class.java)
                producer.produce(rec.outboxId, event)
                    .whenComplete { _, ex ->
                        if (ex == null) outboxRepository.markOutboxRecordsAsProcessed(listOf(rec.outboxId))
                        else outboxRepository.markOutboxRecordsAsFailed(listOf(rec.outboxId))
                    }
            } catch (e: Exception) {
                outboxRepository.markOutboxRecordsAsFailed(listOf(rec.outboxId))
            }
        }
    }
}