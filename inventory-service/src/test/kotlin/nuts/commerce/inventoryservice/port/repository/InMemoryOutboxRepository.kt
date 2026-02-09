//package nuts.commerce.inventoryservice.port.repository
//
//import nuts.commerce.inventoryservice.model.OutboxRecord
//import nuts.commerce.inventoryservice.model.OutboxStatus
//import java.time.Instant
//import java.util.UUID
//import java.util.concurrent.ConcurrentHashMap
//
//class InMemoryOutboxRepository(
//    private val nowProvider: () -> Instant = { Instant.now() }
//) : OutboxRepository {
//    private val store: MutableMap<UUID, OutboxRecord> = ConcurrentHashMap()
//
//    fun clear() = store.clear()
//
//    override fun save(record: OutboxRecord): OutboxRecord {
//        store[record.outboxId] = record
//        return record
//    }
//
//    override fun getOutboxRecordsListByIds(ids: List<UUID>): List<OutboxRecord> =
//        ids.mapNotNull(store::get)
//
//    override fun claimPendingOutboxRecords(limit: Int): List<UUID> {
//        val now = nowProvider()
//        val candidates = store.values
//            .asSequence()
//            .filter { rec ->
//                when (rec.status) {
//                    OutboxStatus.PENDING -> true
//                    OutboxStatus.RETRY_SCHEDULED -> {
//                        val na = rec.nextAttemptAt
//                        na == null || !na.isAfter(now)
//                    }
//
//                    else -> false
//                }
//            }
//            .sortedBy { it.createdAt }
//            .take(limit)
//            .toList()
//
//        candidates.forEach { it.startProcessing(now) }
//
//        return candidates.map { it.outboxId }
//    }
//
//    override fun markOutboxRecordsAsProcessed(ids: List<UUID>) {
//        val now = nowProvider()
//        ids.mapNotNull(store::get).forEach { rec ->
//            if (rec.status == OutboxStatus.PROCESSING) {
//                rec.markPublished(now)
//            }
//        }
//    }
//
//    override fun markOutboxRecordsAsFailed(ids: List<UUID>) {
//        val now = nowProvider()
//        ids.mapNotNull(store::get).forEach { rec ->
//            if (rec.status == OutboxStatus.PROCESSING) {
//                rec.markFailed(now)
//            }
//        }
//    }
//}