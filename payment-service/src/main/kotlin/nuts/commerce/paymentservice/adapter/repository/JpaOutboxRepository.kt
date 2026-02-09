package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.OutboxRecord
import nuts.commerce.paymentservice.port.repository.OutboxRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaOutboxRepository(
    private val outboxJpa: OutboxJpa
) : OutboxRepository {

    override fun save(record: OutboxRecord): OutboxRecord {
        TODO("Not yet implemented")
    }

    override fun findById(id: UUID): OutboxRecord? {
        TODO("Not yet implemented")
    }

    override fun claimAndLockBatchIds(
        batchSize: Int,
        lockedBy: String
    ): List<UUID> {
        TODO("Not yet implemented")
    }
}

interface OutboxJpa : JpaRepository<OutboxRecord, UUID>
