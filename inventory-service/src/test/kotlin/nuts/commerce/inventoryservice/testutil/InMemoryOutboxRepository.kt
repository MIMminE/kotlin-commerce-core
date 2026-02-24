package nuts.commerce.inventoryservice.testutil

import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.port.repository.ClaimOutboxResult
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxRepository : OutboxRepository {
    private val store = ConcurrentHashMap<UUID, OutboxRecord>()

    override fun save(record: OutboxRecord): UUID {
        store[record.outboxId] = record
        return record.outboxId
    }

    override fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): ClaimOutboxResult {
        val now = Instant.now()
        val pendingRecords = store.values
            .filter { it.status == OutboxStatus.PENDING || it.status == OutboxStatus.RETRY_SCHEDULED }
            .filter { it.nextAttemptAt.isBefore(now) || it.nextAttemptAt.equals(now) }
            .take(batchSize)

        val lockedUntil = Instant.now().plusSeconds(300) // 5분 잠금
        val outboxInfos = mutableListOf<OutboxInfo>()

        pendingRecords.forEach { record ->
            record.lockedBy = lockedBy
            record.lockedUntil = lockedUntil
            record.status = OutboxStatus.PROCESSING
            store[record.outboxId] = record

            outboxInfos.add(
                OutboxInfo(
                    outboxId = record.outboxId,
                    orderId = record.orderId,
                    reservationId = record.reservationId,
                    eventType = record.eventType,
                    payload = record.payload
                )
            )
        }

        return ClaimOutboxResult(
            size = outboxInfos.size,
            outboxInfo = outboxInfos
        )
    }

    override fun markPublished(outboxId: UUID, lockedBy: String) {
        val record = store[outboxId] ?: return

        if (record.lockedBy == lockedBy) {
            record.status = OutboxStatus.PUBLISHED
            record.lockedBy = null
            record.lockedUntil = null
            store[outboxId] = record
        }
    }

    override fun markFailed(outboxId: UUID, lockedBy: String) {
        val record = store[outboxId] ?: return

        if (record.lockedBy == lockedBy) {
            record.attemptCount++
            if (record.attemptCount >= 3) {
                record.status = OutboxStatus.FAILED
            } else {
                record.status = OutboxStatus.RETRY_SCHEDULED
                record.nextAttemptAt = Instant.now().plusSeconds((record.attemptCount * 60).toLong())
            }
            record.lockedBy = null
            record.lockedUntil = null
            store[outboxId] = record
        }
    }

    fun clear() {
        store.clear()
    }

    fun getAll(): List<OutboxRecord> {
        return store.values.toList()
    }
}



