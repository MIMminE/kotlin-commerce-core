package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.application.port.repository.PaymentResultRecordRepository.GetOrCreateResult
import nuts.commerce.orderservice.model.infra.PaymentResultRecord
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPaymentResultRecordRepository : PaymentResultRecordRepository {

    private val store = ConcurrentHashMap<UUID, PaymentResultRecord>()

    override fun getOrCreate(record: PaymentResultRecord): GetOrCreateResult {

        val existing = store.putIfAbsent(record.eventId, record)
        return if (existing == null) {
            GetOrCreateResult(record = record, isCreated = true)
        } else {
            GetOrCreateResult(record = record, isCreated = false)
        }
    }

    override fun listByOrder(orderId: UUID): List<PaymentResultRecord> =
        store.values
            .asSequence()
            .filter { it.orderId == orderId }
            .toList()

    override fun save(record: PaymentResultRecord): PaymentResultRecord {
        store[record.eventId] = record
        return record
    }

    fun clear() = store.clear()
}