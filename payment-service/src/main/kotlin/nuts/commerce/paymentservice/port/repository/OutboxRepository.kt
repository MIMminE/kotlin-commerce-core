package nuts.commerce.paymentservice.port.repository

import nuts.commerce.paymentservice.model.EventType
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
    val claimOutboxInfo: List<ClaimOutboxInfo>
) {
    data class ClaimOutboxInfo(
        val outboxId: UUID,
        val orderId: UUID,
        val paymentId: UUID,
        val eventType: EventType,
        val payload: String
    )
}