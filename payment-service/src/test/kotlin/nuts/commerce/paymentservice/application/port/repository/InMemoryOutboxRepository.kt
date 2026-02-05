package nuts.commerce.paymentservice.application.port.repository

import nuts.commerce.paymentservice.model.infra.OutboxRecord
import nuts.commerce.paymentservice.model.infra.OutboxStatus
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOutboxRepository : OutboxRepository {
    private val byId = ConcurrentHashMap<UUID, OutboxRecord>()

    override fun save(record: OutboxRecord): OutboxRecord {
        byId[record.outboxId] = record
        return record
    }

    override fun findById(id: UUID): OutboxRecord? = byId[id]

    override fun findPending(): List<OutboxRecord> =
        byId.values.filter { it.status == OutboxStatus.PENDING || it.status == OutboxStatus.RETRY_SCHEDULED }

    override fun clear() = byId.clear()
}