package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.OutboxInfo
import nuts.commerce.inventoryservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): UUID
    fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): ClaimOutboxResult
    fun markPublished(outboxId: UUID, lockedBy: String)
    fun markFailed(outboxId: UUID, lockedBy: String)
}

data class ClaimOutboxResult(
    val size: Int,
    val outboxInfo: List<OutboxInfo>
)