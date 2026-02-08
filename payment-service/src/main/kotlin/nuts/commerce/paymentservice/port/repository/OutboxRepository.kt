package nuts.commerce.paymentservice.port.repository

import nuts.commerce.paymentservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): OutboxRecord
    fun claimPendingRecords(limit: Int): List<UUID>
    fun getOutboxRecordsByIds(ids: List<UUID>): List<OutboxRecord>
    fun markAsProcessed(id: UUID)
    fun markAsFailed(id: UUID)
}