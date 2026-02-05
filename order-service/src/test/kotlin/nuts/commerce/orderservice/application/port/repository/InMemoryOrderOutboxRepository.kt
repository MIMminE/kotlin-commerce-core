package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.infra.OutboxRecord
import nuts.commerce.orderservice.model.infra.OutboxStatus
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderOutboxRepository(
    private val nowProvider: () -> Instant = { Instant.now() },
) : OrderOutboxRepository {

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
        store.values.filter { it.aggregateId == aggregateId }

    override fun claimReadyToPublishIds(limit: Int): List<UUID> {
        // TODO
        return store.values
            .asSequence()
            .filter { rec ->
                when (rec.status) {
                    OutboxStatus.PENDING -> true
                    OutboxStatus.FAILED -> true
                    OutboxStatus.RETRY_SCHEDULED -> {
                        val na = rec.nextAttemptAt
                        na == null || !na.isAfter(nowProvider())
                    }
                    else -> false
                }
            }
            .sortedBy { it.createdAt }
            .map { it.outboxId }
            .take(limit)
            .toList()
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