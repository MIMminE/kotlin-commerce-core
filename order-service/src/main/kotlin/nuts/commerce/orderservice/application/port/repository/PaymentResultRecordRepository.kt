package nuts.commerce.orderservice.application.port.repository

import nuts.commerce.orderservice.model.integration.PaymentResultRecord
import java.util.UUID

interface PaymentResultRecordRepository {
    fun getOrCreate(record: PaymentResultRecord): GerOrCreateResult
    fun listByOrder(orderId: UUID): List<PaymentResultRecord>

    data class GerOrCreateResult(
        val record: PaymentResultRecord,
        val isCreated: Boolean
    )
}