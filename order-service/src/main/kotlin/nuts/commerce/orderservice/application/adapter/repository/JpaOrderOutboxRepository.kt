package nuts.commerce.orderservice.application.adapter.repository

import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.model.infra.OutboxRecord
import nuts.commerce.orderservice.model.infra.OutboxStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaOrderOutboxRepository(private val outBoxJpa: OrderOutboxJpa) : OrderOutboxRepository {
    override fun save(event: OutboxRecord): OutboxRecord {
        return outBoxJpa.save(event)
    }

    override fun findById(id: UUID): OutboxRecord? {
        return outBoxJpa.findById(id).orElse(null)
    }

    override fun findByIds(ids: List<UUID>): List<OutboxRecord> {
        return outBoxJpa.findAllById(ids).toList()
    }

    override fun findByAggregateId(aggregateId: UUID): List<OutboxRecord> {
        return outBoxJpa.findAll().filter { it.aggregateId == aggregateId }
    }

    override fun claimReadyToPublishIds(limit: Int): List<UUID> {
        val now = Instant.now()
        // fetch candidates whose status is PENDING / FAILED / RETRY_SCHEDULED
        val candidates = outBoxJpa.findByStatusIn(
            listOf(
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                OutboxStatus.RETRY_SCHEDULED
            )
        )

        // filter by nextAttemptAt (null means ready) and order by createdAt (approx via createdAt)
        return candidates
            .asSequence()
            .filter { rec ->
                val na = rec.nextAttemptAt
                na == null || !na.isAfter(now)
            }
            .sortedBy { it.createdAt }
            .map { it.outboxId }
            .take(limit)
            .toList()
    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {
        val rec = outBoxJpa.findById(eventId).orElse(null) ?: return false
        return try {
            if (rec.status != OutboxStatus.PROCESSING) return false
            // use entity method to ensure consistent checks and updatedAt handling
            rec.markPublished(publishedAt)
            outBoxJpa.save(rec)
            true
        } catch (ex: Exception) {
            false
        }
    }

    override fun markFailed(
        eventId: UUID,
        error: String,
        failedAt: Instant
    ): Boolean {
        val rec = outBoxJpa.findById(eventId).orElse(null) ?: return false
        return try {
            if (rec.status != OutboxStatus.PROCESSING) return false
            rec.markFailed(failedAt, error)
            outBoxJpa.save(rec)
            true
        } catch (ex: Exception) {
            false
        }
    }
}

interface OrderOutboxJpa : JpaRepository<OutboxRecord, UUID> {
    fun findByStatusIn(statuses: List<OutboxStatus>): List<OutboxRecord>
}