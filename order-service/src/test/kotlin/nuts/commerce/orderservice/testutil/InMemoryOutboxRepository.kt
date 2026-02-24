package nuts.commerce.orderservice.testutil

import nuts.commerce.orderservice.model.OutboxInfo
import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.model.OutboxStatus
import nuts.commerce.orderservice.port.repository.ClaimOutboxResult
import nuts.commerce.orderservice.port.repository.OutboxRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxRepository(
    private val nowProvider: () -> Instant = { Instant.now() }
) : OutboxRepository {

    private val store: MutableMap<UUID, OutboxRecord> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(record: OutboxRecord): UUID {
        store[record.outboxId] = record
        return record.outboxId
    }

    fun findById(id: UUID): OutboxRecord? {
        return store[id]
    }

    fun findAll(): List<OutboxRecord> {
        return store.values.toList()
    }

    override fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): ClaimOutboxResult {
        val now = nowProvider()

        val eligibleRecords = store.values
            .filter {
                (it.status == OutboxStatus.PENDING || it.status == OutboxStatus.RETRY_SCHEDULED) &&
                it.nextAttemptAt <= now &&
                (it.lockedBy == null || it.lockedUntil == null || it.lockedUntil!! < now)
            }
            .sortedBy { it.nextAttemptAt }
            .take(batchSize)

        val outboxInfo = eligibleRecords.map { record ->
            record.lockedBy = lockedBy
            record.lockedUntil = now.plusSeconds(300) // 5분 lock
            record.status = OutboxStatus.PROCESSING
            record.attemptCount++

            OutboxInfo(
                outboxId = record.outboxId,
                orderId = record.orderId,
                eventType = record.eventType,
                payload = record.payload
            )
        }

        return ClaimOutboxResult(
            size = outboxInfo.size,
            outboxInfo = outboxInfo
        )
    }

    override fun markPublished(outboxId: UUID, lockedBy: String) {
        val record = store[outboxId] ?: return
        if (record.lockedBy == lockedBy && record.status == OutboxStatus.PROCESSING) {
            record.status = OutboxStatus.PUBLISHED
            record.lockedBy = null
            record.lockedUntil = null
        }
    }

    override fun markFailed(outboxId: UUID, lockedBy: String) {
        val record = store[outboxId] ?: return
        if (record.lockedBy == lockedBy && record.status == OutboxStatus.PROCESSING) {
            record.status = OutboxStatus.FAILED
            record.lockedBy = null
            record.lockedUntil = null
        }
    }
}

