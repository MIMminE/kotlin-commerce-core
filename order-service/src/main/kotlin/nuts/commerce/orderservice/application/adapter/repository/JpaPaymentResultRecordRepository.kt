package nuts.commerce.orderservice.application.adapter.repository

import nuts.commerce.orderservice.application.port.repository.PaymentResultRecordRepository
import nuts.commerce.orderservice.model.infra.PaymentResultRecord
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaPaymentResultRecordRepository(
    private val paymentResultRecordJpa: PaymentResultRecordJpa
) : PaymentResultRecordRepository {

    override fun getOrCreate(record: PaymentResultRecord): PaymentResultRecordRepository.GetOrCreateResult {
        return try {
            PaymentResultRecordRepository.GetOrCreateResult(paymentResultRecordJpa.save(record), true)
        } catch (e: DataIntegrityViolationException) {
            val resultRecord = (paymentResultRecordJpa.findByEventId(record.eventId)
                ?: throw e)
            PaymentResultRecordRepository.GetOrCreateResult(resultRecord, false)
        }
    }

    override fun listByOrder(orderId: UUID): List<PaymentResultRecord> {
        TODO("Not yet implemented")
    }

    override fun save(record: PaymentResultRecord): PaymentResultRecord {
        TODO("Not yet implemented")
    }
}

interface PaymentResultRecordJpa : JpaRepository<PaymentResultRecord, UUID> {
    fun findByEventId(eventId: UUID): PaymentResultRecord?
}