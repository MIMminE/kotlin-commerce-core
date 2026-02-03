package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.integration.OrderOutboxRecord
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderOutboxRepository(
    private val nowProvider: () -> Instant = { Instant.now() },
    private val maxRetries: Int = 3,
) : OrderOutboxRepository {

    private val store: MutableMap<UUID, OrderOutboxRecord> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(event: OrderOutboxRecord): OrderOutboxRecord {
        store[event.id] = event
        return event
    }

    override fun findById(id: UUID): OrderOutboxRecord? =
        store[id]

    override fun findByIds(ids: List<UUID>): List<OrderOutboxRecord> =
        ids.mapNotNull(store::get)

    override fun findByAggregateId(aggregateId: UUID): List<OrderOutboxRecord> =
        store.values.filter { it.aggregateId == aggregateId }

    override fun claimReadyToPublishIds(limit: Int): List<UUID> {
        val now = nowProvider()

        // ready인 것만 골라서, PROCESSING으로 바꾸고 반환
        val ready = store.values
            .asSequence()
            .filter { it.isReady(now) }
            // nextRetryAt이 null이 아니란 보장이 isReady에 있음
            .sortedBy { it.nextRetryAt }
            .take(limit)
            .toList()

        ready.forEach { it.forceStatus(OrderOutboxRecord.OutboxStatus.PROCESSING) }

        return ready.map { it.id }
    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {
        val event = store[eventId] ?: return false

        // "claim -> publish" 흐름을 강제(동시성 테스트에도 유리)
        if (event.status != OrderOutboxRecord.OutboxStatus.PROCESSING) return false

        event.markPublished()
        return true
    }

    override fun markFailed(eventId: UUID, error: String, failedAt: Instant): Boolean {
        val event = store[eventId] ?: return false
        if (event.status != OrderOutboxRecord.OutboxStatus.PROCESSING) return false

        event.markFailed(
            error = error,
            now = failedAt,
            maxRetries = maxRetries,
        )

        return true
    }

    private fun OrderOutboxRecord.forceStatus(status: OrderOutboxRecord.OutboxStatus) {
        val field = OrderOutboxRecord::class.java.getDeclaredField("status")
        field.isAccessible = true
        field.set(this, status)
    }
}