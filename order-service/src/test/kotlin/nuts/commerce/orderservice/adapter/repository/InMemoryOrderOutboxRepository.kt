package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.application.repository.OrderOutboxRepository
import nuts.commerce.orderservice.domain.core.OrderOutboxEvent
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderOutboxRepository(
    private val maxRetries: Int = 10,
    private val baseDelaySeconds: Long = 1,
    private val maxDelaySeconds: Long = 60,
) : OrderOutboxRepository {

    private val store = ConcurrentHashMap<UUID, OrderOutboxEvent>()

    override fun save(event: OrderOutboxEvent): OrderOutboxEvent {
        store[event.id] = event
        return event
    }

    /**
     * "unpublished"를 엄밀히 "아직 발행 안 됨 + 지금 시도 가능한 것"으로 해석(권장)
     */
    override fun findUnpublished(limit: Int): List<OrderOutboxEvent> {
        val now = Instant.now()
        return store.values
            .asSequence()
            .filter { it.status != OutboxStatus.PUBLISHED }
            .filter { it.isReadyToRetry(now) }
            .take(limit)
            .toList()
    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {
        var changed = false

        store.computeIfPresent(eventId) { _, current ->
            if (current.status != OutboxStatus.PUBLISHED) {
                current.markPublished(publishedAt)
                changed = true
            }
            current
        }

        return changed
    }

    override fun markFailed(eventId: UUID, error: String, failedAt: Instant): Boolean {
        var changed = false

        store.computeIfPresent(eventId) { _, current ->
            if (current.status != OutboxStatus.PUBLISHED) {
                current.markFailed(
                    error = error,
                    now = failedAt,
                    maxRetries = maxRetries,
                    baseDelaySeconds = baseDelaySeconds,
                    maxDelaySeconds = maxDelaySeconds,
                )
                changed = true
            }
            current
        }

        return changed
    }

    // 테스트 편의
    fun findById(id: UUID): OrderOutboxEvent? = store[id]
    fun clear() = store.clear()
}