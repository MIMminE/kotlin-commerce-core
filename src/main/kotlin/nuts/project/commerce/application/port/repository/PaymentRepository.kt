package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.core.Payment
import nuts.project.commerce.domain.core.payment.PaymentStatus
import java.util.UUID

interface PaymentRepository {

    fun findById(paymentId: UUID): Payment?
    fun save(payment: Payment): Payment
    fun findByIdAndStatus(paymentId: UUID, status: PaymentStatus): Payment?
    fun update(paymentId: UUID, pgProvider: String, pgSessionId: String)
    fun updateStatus(paymentId: UUID, status: PaymentStatus)
}