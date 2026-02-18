package nuts.commerce.paymentservice.port.repository

import nuts.commerce.paymentservice.model.OutboxInfo
import nuts.commerce.paymentservice.model.OutboxRecord
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