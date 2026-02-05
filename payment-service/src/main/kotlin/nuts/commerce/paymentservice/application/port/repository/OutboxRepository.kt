package nuts.commerce.paymentservice.application.port.repository

import nuts.commerce.paymentservice.model.infra.OutboxRecord
import java.util.UUID

interface OutboxRepository {
    fun save(record: OutboxRecord): OutboxRecord
    fun findById(id: UUID): OutboxRecord?
    fun findPending(): List<OutboxRecord>
    fun clear()
}