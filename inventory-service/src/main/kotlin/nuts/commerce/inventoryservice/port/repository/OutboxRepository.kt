package nuts.commerce.inventoryservice.port.repository

import nuts.commerce.inventoryservice.model.OutboxRecord
import java.util.UUID

interface OutboxRepository{
    fun save(record: OutboxRecord): OutboxRecord
    fun getOutboxRecordsListByIds(ids: List<UUID>): List<OutboxRecord>
    fun claimPendingOutboxRecords(limit: Int): List<UUID>
    fun markOutboxRecordsAsProcessed(ids: List<UUID>)
    fun markOutboxRecordsAsFailed(ids: List<UUID>)
}