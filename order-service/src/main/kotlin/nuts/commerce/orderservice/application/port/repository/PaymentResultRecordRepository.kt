package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.infra.PaymentResultRecord
import java.util.UUID

interface PaymentResultRecordRepository {
    fun getOrCreate(record: PaymentResultRecord): GetOrCreateResult
    fun listByOrder(orderId: UUID): List<PaymentResultRecord>
    fun save(record: PaymentResultRecord): PaymentResultRecord

    data class GetOrCreateResult(
        val record: PaymentResultRecord,
        val isCreated: Boolean
    )
}