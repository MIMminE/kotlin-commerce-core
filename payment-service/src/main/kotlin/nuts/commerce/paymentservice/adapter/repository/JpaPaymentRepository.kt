package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaPaymentRepository(private val paymentJpa: PaymentJpa) : PaymentRepository {
    override fun save(payment: Payment): Payment {
        return paymentJpa.save(payment)
    }

    override fun findById(paymentId: UUID): Payment? {
        return paymentJpa.findById(paymentId).orElse(null)
    }

    override fun findByOrderId(orderId: UUID): Payment? {
        return paymentJpa.findByOrderId(orderId)
    }
}

interface PaymentJpa : JpaRepository<Payment, UUID> {
    fun findByOrderId(orderId: UUID): Payment?
}