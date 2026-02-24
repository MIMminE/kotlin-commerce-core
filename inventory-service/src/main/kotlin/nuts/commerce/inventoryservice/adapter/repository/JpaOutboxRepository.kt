package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
import nuts.commerce.inventoryservice.port.repository.ClaimOutboxResult
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
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
class JpaOutboxRepository(private val outboxJpa: OutboxJpa) : OutboxRepository {
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
            outboxStatus = OutboxStatus.PENDING
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
                    reservationId = record.reservationId,
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
            throw IllegalStateException("Failed to mark outbox as published. outboxId=$outboxId, lockedBy=$lockedBy")
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
            throw IllegalStateException("Failed to mark outbox as failed. outboxId=$outboxId, lockedBy=$lockedBy")
        }
    }
}

interface OutboxJpa : JpaRepository<OutboxRecord, UUID> {

    @Query(
        """
        select o.outboxId
          from OutboxRecord o 
         where o.status = :outboxStatus
           and o.nextAttemptAt <= :now
           and (o.lockedUntil is null or o.lockedUntil < :now)
         order by o.createdAt
    """
    )
    fun findClaimCandidates(
        @Param("now") now: Instant, pageable: Pageable, @Param("outboxStatus") outboxStatus: OutboxStatus
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
               o.lockedUntil = null
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