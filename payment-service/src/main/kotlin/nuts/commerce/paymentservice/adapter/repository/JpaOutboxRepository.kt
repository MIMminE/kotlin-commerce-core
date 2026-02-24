package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.model.OutboxStatus
import nuts.commerce.paymentservice.port.repository.ClaimOutboxResult
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class JpaOutboxRepository(
    private val outboxJpa: OutboxJpa
) : OutboxRepository {

    override fun save(record: OutboxRecord): UUID {
        return outboxJpa.saveAndFlush(record).outboxId
    }

    override fun claimAndLockBatchIds(
        batchSize: Int,
        lockedBy: String
    ): ClaimOutboxResult {

        val now = Instant.now()
        val leaseUntil = now.plusSeconds(60)

        val candidates = outboxJpa.findClaimCandidates(
            now = now,
            pageable = Pageable.ofSize(batchSize),
            outboxStatusList = listOf(OutboxStatus.PENDING, OutboxStatus.RETRY_SCHEDULED)
        )
        if (candidates.isEmpty()) return ClaimOutboxResult(
            size = 0,
            outboxInfo = emptyList()
        )

        outboxJpa.claimBatch(
            ids = candidates,
            workerId = lockedBy,
            expectedStatus = OutboxStatus.PENDING,
            newStatus = OutboxStatus.PROCESSING,
            now = now,
            leaseUntil = leaseUntil
        )

        val claimOutboxInfo = outboxJpa.findClaimed(
            workerId = lockedBy,
            outboxStatus = OutboxStatus.PROCESSING,
            leaseUntil = leaseUntil
        )
            .map { record ->
                OutboxInfo(
                    outboxId = record.outboxId,
                    orderId = record.orderId,
                    paymentId = record.paymentId,
                    eventType = record.eventType,
                    payload = record.payload
                )
            }

        return ClaimOutboxResult(
            size = claimOutboxInfo.size,
            outboxInfo = claimOutboxInfo
        )
    }

    override fun markPublished(outboxId: UUID, lockedBy: String) {
        val now = Instant.now()
        val updatedRows = outboxJpa.markOutboxStatus(
            outboxId = outboxId,
            lockedBy = lockedBy,
            now = now,
            expectedStatus = OutboxStatus.PROCESSING,
            newStatus = OutboxStatus.PUBLISHED
        )
        if (updatedRows == 0) {
            throw IllegalStateException("Failed to mark outbox as PUBLISHED for outboxId: $outboxId")
        }
    }

    override fun markFailed(outboxId: UUID, lockedBy: String) {
        val now = Instant.now()
        val updatedRows = outboxJpa.markOutboxStatus(
            outboxId = outboxId,
            lockedBy = lockedBy,
            now = now,
            expectedStatus = OutboxStatus.PROCESSING,
            newStatus = OutboxStatus.FAILED
        )
        if (updatedRows == 0) {
            throw IllegalStateException("Failed to mark outbox as FAILED for outboxId: $outboxId")
        }
    }

}

interface OutboxJpa : JpaRepository<OutboxRecord, UUID> {

    @Query(
        """
        select o.outboxId
          from OutboxRecord o 
         where o.status in :outboxStatusList
           and o.nextAttemptAt <= :now
           and (o.lockedUntil is null or o.lockedUntil < :now)
         order by o.createdAt
    """
    )
    fun findClaimCandidates(
        @Param("now") now: Instant, pageable: Pageable, @Param("outboxStatusList") outboxStatusList: List<OutboxStatus>
    ): List<UUID>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxRecord o
           set o.lockedBy = :workerId,
               o.status = :newStatus,
               o.lockedUntil = :leaseUntil
         where o.outboxId in :ids
           and o.status = :expectedStatus
           and (o.lockedUntil is null or o.lockedUntil < :now)
    """
    )
    fun claimBatch(
        @Param("ids") ids: List<UUID>,
        @Param("workerId") workerId: String,
        @Param("expectedStatus") expectedStatus: OutboxStatus,
        @Param("newStatus") newStatus: OutboxStatus,
        @Param("now") now: Instant,
        @Param("leaseUntil") leaseUntil: Instant
    ): Int

    @Query(
        """
        select o
          from OutboxRecord o
         where o.lockedBy = :workerId
           and o.status = :outboxStatus
           and o.lockedUntil = :leaseUntil
    """
    )
    fun findClaimed(
        @Param("workerId") workerId: String,
        @Param("outboxStatus") outboxStatus: OutboxStatus,
        @Param("leaseUntil") leaseUntil: Instant
    ): List<OutboxRecord>


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxRecord o
           set o.status = :newStatus,
               o.lockedBy = null,
               o.lockedUntil = null,
               o.attemptCount = o.attemptCount + 1
         where o.outboxId = :outboxId
           and o.lockedBy = :lockedBy
           and o.status = :expectedStatus
        """
    )
    fun markOutboxStatus(
        outboxId: UUID,
        lockedBy: String,
        now: Instant,
        expectedStatus: OutboxStatus,
        newStatus: OutboxStatus
    ): Int

}
