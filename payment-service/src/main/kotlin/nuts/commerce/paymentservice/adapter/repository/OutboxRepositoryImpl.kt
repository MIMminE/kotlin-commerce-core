package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class OutboxRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val jpaRepository: JpaRepository<OutboxRecord, UUID>
) : OutboxRepository {

    override fun save(record: OutboxRecord): OutboxRecord {
        return jpaRepository.save(record)
    }

    @Transactional
    override fun claimPendingRecords(limit: Int): List<UUID> {
        val sql = """
            WITH cte AS (
              SELECT outbox_id
              FROM payment_outbox_records
              WHERE (status = 'PENDING' OR (status = 'RETRY_SCHEDULED' AND (next_attempt_at IS NULL OR next_attempt_at <= ?)))
              ORDER BY created_at
              FOR UPDATE SKIP LOCKED
              LIMIT ?
            )
            UPDATE payment_outbox_records
            SET status = 'PROCESSING', updated_at = ?, version = COALESCE(version,0) + 1
            WHERE outbox_id IN (SELECT outbox_id FROM cte)
            RETURNING outbox_id
        """.trimIndent()

        val now = Timestamp.from(Instant.now())
        val ids = jdbcTemplate.queryForList(sql, UUID::class.java, now, limit, now)
        return ids
    }

    override fun getOutboxRecordsByIds(ids: List<UUID>): List<OutboxRecord> {
        return jpaRepository.findAllById(ids)
    }

    @Transactional
    override fun markAsProcessed(id: UUID) {
        val now = Timestamp.from(Instant.now())
        val sql = """
            UPDATE payment_outbox_records
            SET status = 'PUBLISHED', updated_at = ?, version = COALESCE(version,0) + 1
            WHERE outbox_id = ? AND status = 'PROCESSING'
        """.trimIndent()

        val updated = jdbcTemplate.update(sql, now, id)
        if (updated != 1) {
            throw OptimisticLockingFailureException("Expected to mark 1 outbox record as processed for id=$id but updated $updated - concurrent modification detected")
        }
    }

    @Transactional
    override fun markAsFailed(id: UUID) {
        val now = Timestamp.from(Instant.now())
        val sql = """
            UPDATE payment_outbox_records
            SET status = 'FAILED', updated_at = ?, version = COALESCE(version,0) + 1
            WHERE outbox_id = ? AND status = 'PROCESSING'
        """.trimIndent()

        val updated = jdbcTemplate.update(sql, now, id)
        if (updated != 1) {
            throw OptimisticLockingFailureException("Expected to mark 1 outbox record as failed for id=$id but updated $updated - concurrent modification detected")
        }
    }
}

interface JpaOutboxRepository : JpaRepository<OutboxRecord, UUID>
