package nuts.commerce.paymentservice.port.repository

import nuts.commerce.paymentservice.model.Payment
import java.util.UUID

interface PaymentRepository {
    fun save(payment: Payment): UUID
    fun findById(paymentId: UUID): Payment?
    fun findPaymentIdForIdempotencyKey(orderId: UUID, idempotencyKey: UUID): UUID?
    fun findByOrderId(orderId: UUID): Payment?
}