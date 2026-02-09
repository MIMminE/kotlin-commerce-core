package nuts.commerce.paymentservice.port.repository

import nuts.commerce.paymentservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): OutboxRecord
    fun findById(id: UUID): OutboxRecord?
    fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): List<UUID>
}