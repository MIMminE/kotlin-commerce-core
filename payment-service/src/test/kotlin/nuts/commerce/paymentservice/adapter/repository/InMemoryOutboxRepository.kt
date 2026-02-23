package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.OutboxStatus
import nuts.commerce.paymentservice.port.repository.ClaimOutboxResult
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import java.time.Instant
import java.util.UUID

class InMemoryOutboxRepository : OutboxRepository {
    private val records = LinkedHashMap<UUID, OutboxRecord>()

    override fun save(record: OutboxRecord): UUID {
        records[record.outboxId] = record
        return record.outboxId
    }

    override fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): ClaimOutboxResult {
        val now = Instant.now()
        val leaseUntil = now.plusSeconds(60)

        val candidates = records.values
            .asSequence()
            .filter { it.status == OutboxStatus.PENDING }
            .filter { it.nextAttemptAt <= now }
            .filter { it.lockedUntil == null || it.lockedUntil!!.isBefore(now) }
            .sortedBy { it.createdAt }
            .take(batchSize)
            .toList()

        if (candidates.isEmpty()) {
            return ClaimOutboxResult(size = 0, outboxInfo = emptyList())
        }

        candidates.forEach { record ->
            record.lockedBy = lockedBy
            record.status = OutboxStatus.PROCESSING
            record.lockedUntil = leaseUntil
        }

        val outboxInfo = candidates.map { record ->
            OutboxInfo(
                outboxId = record.outboxId,
                orderId = record.orderId,
                paymentId = record.paymentId,
                eventType = record.eventType,
                payload = record.payload
            )
        }

        return ClaimOutboxResult(size = outboxInfo.size, outboxInfo = outboxInfo)
    }

    override fun markPublished(outboxId: UUID, lockedBy: String) {
        val record = records[outboxId]
        if (record == null || record.status != OutboxStatus.PROCESSING || record.lockedBy != lockedBy) {
            throw IllegalStateException("Failed to mark outbox as PUBLISHED for outboxId: $outboxId")
        }
        record.status = OutboxStatus.PUBLISHED
        record.lockedBy = null
        record.lockedUntil = null
    }

    override fun markFailed(outboxId: UUID, lockedBy: String) {
        val record = records[outboxId]
        if (record == null || record.status != OutboxStatus.PROCESSING || record.lockedBy != lockedBy) {
            throw IllegalStateException("Failed to mark outbox as FAILED for outboxId: $outboxId")
        }
        record.status = OutboxStatus.FAILED
        record.lockedBy = null
        record.lockedUntil = null
    }

    fun clear() {
        records.clear()
    }
}