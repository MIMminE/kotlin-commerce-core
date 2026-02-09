package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.model.OutboxStatus
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
    override fun save(record: OutboxRecord): OutboxRecord {
        return outboxJpa.saveAndFlush(record)
    }

    override fun findById(id: UUID): OutboxRecord? {
        return outboxJpa.findById(id).orElse(null)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun claimBatchAndLock(
        batchSize: Int,
        lockedBy: String
    ): List<UUID> {

        val now = Instant.now()
        val leaseUntil = now.plusSeconds(60)

        val candidates = outboxJpa.findClaimCandidates(
            now = now,
            pageable = Pageable.ofSize(batchSize),
            outboxStatus = OutboxStatus.PENDING
        )

        if (candidates.isEmpty()) return emptyList()

        outboxJpa.claimBatch(
            ids = candidates,
            workerId = lockedBy,
            expectedStatus = OutboxStatus.PENDING,
            newStatus = OutboxStatus.PROCESSING,
            now = now,
            leaseUntil = leaseUntil
        )

        return outboxJpa.findClaimed(
            workerId = lockedBy,
            outboxStatus = OutboxStatus.PROCESSING,
            leaseUntil = leaseUntil
        )
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
        select o.outboxId
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
    ): List<UUID>
}