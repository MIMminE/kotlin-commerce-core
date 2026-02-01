package nuts.commerce.orderservice.infra.repository

import nuts.commerce.orderservice.application.repository.OrderOutboxRepository
import nuts.commerce.orderservice.domain.core.OrderOutboxEvent
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaOrderOutboxRepository(private val outBoxJpa: OrderOutboxJpa) : OrderOutboxRepository {
    override fun save(event: OrderOutboxEvent): OrderOutboxEvent {
        return outBoxJpa.save(event)
    }

    override fun findUnpublished(limit: Int): List<OrderOutboxEvent> {
        return outBoxJpa.findByPublishedFalseOrderByCreatedAtAsc(pageable = Pageable.ofSize(limit))
    }

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

interface OrderOutboxJpa : JpaRepository<OrderOutboxEvent, UUID> {
    fun findByPublishedFalseOrderByCreatedAtAsc(pageable: Pageable): List<OrderOutboxEvent>
}