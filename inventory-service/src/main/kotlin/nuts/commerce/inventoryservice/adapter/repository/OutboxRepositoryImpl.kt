package nuts.commerce.inventoryservice.adapter.repository

import nuts.commerce.inventoryservice.model.OutboxRecord
import nuts.commerce.inventoryservice.port.repository.OutboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.util.*

@Repository
class OutboxRepositoryImpl(
    private val jdbcTemplate: JdbcTemplate,
    private val jpaRepository: JpaRepository<OutboxRecord, UUID>

) : OutboxRepository {

    override fun save(record: OutboxRecord): OutboxRecord {
        jpaRepository.save(record)
        return record
    }

    override fun getOutboxRecordsListByIds(ids: List<UUID>): List<OutboxRecord> {
        return jpaRepository.findAllById(ids)
    }

    @Transactional
    override fun claimPendingOutboxRecords(limit: Int): List<UUID> {
        val sql = """
            SELECT outbox_id FROM inventory_outbox_records
            WHERE (status = 'PENDING' OR (status = 'RETRY_SCHEDULED' AND (next_attempt_at IS NULL OR next_attempt_at <= now())))
            ORDER BY created_at
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        """.trimIndent()

        val ids: List<UUID> = jdbcTemplate.queryForList(sql, UUID::class.java, limit)

        return ids
    }

    @Transactional
    override fun markOutboxRecordsAsProcessed(ids: List<UUID>) {
        if (ids.isEmpty()) return
        val sql = "UPDATE inventory_outbox_records SET status = 'PUBLISHED', updated_at = now() WHERE outbox_id = ?"
        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setObject(1, ids[i])
            }

            override fun getBatchSize(): Int = ids.size
        })
    }

    @Transactional
    override fun markOutboxRecordsAsFailed(ids: List<UUID>) {
        if (ids.isEmpty()) return
        val sql =
            "UPDATE inventory_outbox_records SET status = 'FAILED', attempts = attempts + 1, updated_at = now() WHERE outbox_id = ?"
        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                ps.setObject(1, ids[i])
            }

            override fun getBatchSize(): Int = ids.size
        })
    }
}

interface JpaOutboxRepository : JpaRepository<OutboxRecord, UUID>