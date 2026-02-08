package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.port.repository.OutboxClaimRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class JpaOutboxClaimRepository(
    private val outboxJpa: OutboxJpa
) : OutboxClaimRepository {

    override fun claimOutboxRecords(limit: Int): List<UUID> {
        val now = Instant.now()

        outboxJpa.claimByLimit(now, limit)

        return outboxJpa.findClaimedIdsByUpdatedAt(now)
    }
}

interface OutboxJpa : JpaRepository<OutboxRecord, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update inventory_outbox_records
           set status = 'PROCESSING', updated_at = :now
         where outbox_id in (
            select outbox_id from inventory_outbox_records
             where status in ('PENDING','RETRY_SCHEDULED')
               and (next_attempt_at is null or next_attempt_at <= :now)
             order by created_at
             limit :limit
         )
        """,
        nativeQuery = true
    )
    fun claimByLimit(@Param("now") now: Instant, @Param("limit") limit: Int): Int

    @Query(
        value = "select outbox_id from inventory_outbox_records where status = 'PROCESSING' and updated_at = :now",
        nativeQuery = true
    )
    fun findClaimedIdsByUpdatedAt(@Param("now") now: Instant): List<UUID>
}