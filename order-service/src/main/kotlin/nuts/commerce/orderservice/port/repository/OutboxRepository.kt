package nuts.commerce.orderservice.port.repository

import nuts.commerce.orderservice.model.OutboxInfo
import nuts.commerce.orderservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): UUID
    fun claimAndLockBatchIds(batchSize: Int, lockedBy: String): ClaimOutboxResult
    fun markPublished(outboxId: UUID, lockedBy: String)
    fun markFailed(outboxId: UUID, lockedBy: String)
}

data class ClaimOutboxResult(
    val size: Int,
    val claimOutboxInfo: List<OutboxInfo>
)