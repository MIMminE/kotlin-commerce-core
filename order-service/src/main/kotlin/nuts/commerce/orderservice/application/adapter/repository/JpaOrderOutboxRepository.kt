package nuts.commerce.orderservice.application.adapter.repository

import nuts.commerce.orderservice.application.port.repository.OrderOutboxRepository
import nuts.commerce.orderservice.model.integration.OrderOutboxRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class JpaOrderOutboxRepository(private val outBoxJpa: OrderOutboxJpa) : OrderOutboxRepository {
    override fun save(event: OrderOutboxRecord): OrderOutboxRecord {
        return outBoxJpa.save(event)
    }

    override fun findById(id: UUID): OrderOutboxRecord? {
        return outBoxJpa.findById(id).orElse(null)
    }

    override fun findByIds(ids: List<UUID>): List<OrderOutboxRecord> {
        return outBoxJpa.findAllById(ids).toList()
    }

    override fun findByAggregateId(aggregateId: UUID): List<OrderOutboxRecord> {
        return outBoxJpa.findAll().filter { it.aggregateId == aggregateId }
    }

    override fun claimReadyToPublishIds(limit: Int): List<UUID> {
        // 퍼블리싱 대상 조회하는 메서드이며, 대상의 상태가 팬딩또는 페일일때 조회되어야 함
        outBoxJpa.findByStatusIn(
            listOf(
                OrderOutboxRecord.OutboxStatus.PENDING,
                OrderOutboxRecord.OutboxStatus.FAILED
            )
        )
    }

    override fun tryMarkPublished(eventId: UUID, publishedAt: Instant): Boolean {

    }

    override fun markFailed(
        eventId: UUID,
        error: String,
        failedAt: Instant
    ): Boolean {

    }
}

interface OrderOutboxJpa : JpaRepository<OrderOutboxRecord, UUID> {
    fun findByStatusIn(statuses: List<OrderOutboxRecord.OutboxStatus>): List<OrderOutboxRecord>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OrderOutboxRecord o
           set o.status = OrderOutboxRecord.OutboxStatus.PUBLISHED,
               o.updatedAt = :now
         where o.id = :id
           and o.status = nuts.commerce.orderservice.model.domain.OrderOutboxRecord.OutboxStatus.PROCESSING
    """
    )
    fun markPublishedIfProcessing(
        @Param("id") id: UUID,
        @Param("publishedAt") publishedAt: Instant,
        @Param("now") now: Instant
    ): Int
}