package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): OutboxRecord
    fun findById(id: UUID): OutboxRecord?
    fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): List<UUID>

}
