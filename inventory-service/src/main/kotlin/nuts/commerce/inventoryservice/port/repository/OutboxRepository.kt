package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.EventType
import nuts.commerce.inventoryservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): OutboxRecord
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
        val reservationId: UUID,
        val eventType: EventType,
        val payload: String
    )
}