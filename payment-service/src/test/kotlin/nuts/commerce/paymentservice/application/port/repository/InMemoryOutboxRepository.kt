package nuts.commerce.paymentservice.application.port.repository


import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.OutboxStatus
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxRepository(
    private val nowProvider: () -> Instant = { Instant.now() }
) : OutboxRepository {

    private val store: MutableMap<UUID, OutboxRecord> = ConcurrentHashMap()

    fun clear() = store.clear()

    fun saveForTest(rec: OutboxRecord) {
        store[rec.outboxId] = rec
    }

    override fun save(record: OutboxRecord): OutboxRecord {
        store[record.outboxId] = record
        return record
    }

    override fun claimPendingRecords(limit: Int): List<UUID> {
        val now = nowProvider()
        val candidates = store.values
            .asSequence()
            .filter { rec ->
                when (rec.status) {
                    OutboxStatus.PENDING -> true
                    OutboxStatus.RETRY_SCHEDULED -> {
                        val na = rec.nextAttemptAt
                        na == null || !na.isAfter(now)
                    }

                    else -> false
                }
            }
            .sortedBy { it.createdAt }
            .take(limit)
            .toList()

        candidates.forEach { it.startProcessing(now) }

        return candidates.map { it.outboxId }
    }

    override fun getOutboxRecordsByIds(ids: List<UUID>): List<OutboxRecord> = ids.mapNotNull { store[it] }

    override fun markAsProcessed(id: UUID) {
        val now = nowProvider()
        store[id]?.let { rec ->
            if (rec.status == OutboxStatus.PROCESSING) {
                rec.markPublished(now)
            }
        }
    }

    override fun markAsFailed(id: UUID) {
        val now = nowProvider()
        store[id]?.let { rec ->
            if (rec.status == OutboxStatus.PROCESSING) {
                rec.markFailed(now)
            }
        }
    }

}