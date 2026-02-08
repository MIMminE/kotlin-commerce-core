package nuts.commerce.paymentservice.application.port.repository

import nuts.commerce.paymentservice.model.InboxRecord
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

class InMemoryInboxRepository : InboxRepository {
    private val store: MutableMap<UUID, InboxRecord> = ConcurrentHashMap()
    private val claimed: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    override fun save(record: InboxRecord): InboxRecord {
        store[record.inboxId] = record
        return record
    }

    override fun claimPendingInboxRecords(limit: Int): List<UUID> {
        val out = ArrayList<UUID>()
        synchronized(this) {
            for ((id, rec) in store) {
                if (out.size >= limit) break
                if (rec.status.name == "RECEIVED" && claimed.add(id)) {
                    out.add(id)
                }
            }
        }
        return out
    }

    override fun getInboxRecordsByIds(ids: List<UUID>): List<InboxRecord> = ids.mapNotNull { store[it] }

    override fun markOutboxRecordsAsProcessed(ids: List<UUID>) {
        val now = Instant.now()
        for (id in ids) {
            claimed.remove(id)
            store[id]?.let { rec ->
                try {
                    rec.markProcessed(now)
                } catch (_: Throwable) {
                }
                store[id] = rec
            }
        }
    }

    override fun markOutboxRecordsAsFailed(ids: List<UUID>) {
        val now = Instant.now()
        for (id in ids) {
            claimed.remove(id)
            store[id]?.let { rec ->
                rec.markForRetry(now)
                store[id] = rec
            }
        }
    }

    fun clear() = store.clear()
}