package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.OutboxRecord
import nuts.commerce.orderservice.model.OutboxStatus
import nuts.commerce.orderservice.port.repository.OutboxRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxRepository(
    private val nowProvider: () -> Instant = { Instant.now() },
) : OutboxRepository {

    private val store: MutableMap<UUID, OutboxRecord> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(event: OutboxRecord): OutboxRecord {
        store[event.outboxId] = event
        return event
    }

    override fun findById(id: UUID): OutboxRecord? =
        store[id]

    override fun findByIds(ids: List<UUID>): List<OutboxRecord> =
        ids.mapNotNull(store::get)

    override fun findByAggregateId(aggregateId: UUID): List<OutboxRecord> =
        store.values.filter { it.orderId == aggregateId }

    override fun claimReadyToPublishIds(limit: Int): List<UUID> {
        val now = nowProvider()
        val candidates = store.values
            .asSequence()
            .filter { rec ->
                when (rec.status) {
                    OutboxStatus.PENDING -> true
                    OutboxStatus.FAILED -> true
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

        // mark selected records as PROCESSING
        candidates.forEach { rec ->
            rec.startProcessing(now)
            store[rec.outboxId] = rec
        }

        return candidates.map { it.outboxId }
    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {
        val event = store[eventId] ?: return false

        if (event.status != OutboxStatus.PROCESSING) return false

        event.markPublished(now = publishedAt)
        return true
    }

    override fun markFailed(eventId: UUID, error: String, failedAt: Instant): Boolean {
        val event = store[eventId] ?: return false
        if (event.status != OutboxStatus.PROCESSING) return false

        event.markFailed(
            error = error,
            now = failedAt
        )

        return true
    }

    private fun OutboxRecord.forceStatus(status: OutboxStatus) {
        val field = OutboxRecord::class.java.getDeclaredField("status")
        field.isAccessible = true
        field.set(this, status)
    }
}