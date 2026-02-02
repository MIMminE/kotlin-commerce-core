package nuts.commerce.orderservice.application.adapter.repository

import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.model.integration.OrderOutboxRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaOrderOutboxRepository(private val outBoxJpa: OrderOutboxJpa) : OrderOutboxRepository {
    override fun save(event: OrderOutboxRecord): OrderOutboxRecord {
        return outBoxJpa.save(event)
    }

    override fun lockReadyToPublishIds(limit: Int): List<UUID> {
        TODO("Not yet implemented")
    }

    override fun findByIds(ids: List<UUID>): List<OrderOutboxRecord> {
        TODO("Not yet implemented")
    }

//    override fun findUnpublished(limit: Int): List<OrderOutboxEvent> {
//        return outBoxJpa.findByPublishedFalseOrderByCreatedAtAsc(pageable = Pageable.ofSize(limit))
//    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {
        TODO("Not yet implemented")
    }

    override fun markFailed(
        eventId: UUID,
        error: String,
        failedAt: Instant
    ): Boolean {
        TODO("Not yet implemented")
    }
}

interface OrderOutboxJpa : JpaRepository<OrderOutboxRecord, UUID> {
}