package nuts.commerce.paymentservice.application.port.repository

import nuts.commerce.paymentservice.model.domain.Payment
import java.util.UUID

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findById(paymentId: UUID): Payment?
    fun findByOrderId(orderId: UUID): Payment?
}