package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class JpaPaymentRepository(private val paymentJpa: PaymentJpa) : PaymentRepository {
    override fun save(payment: Payment): UUID {
        return paymentJpa.saveAndFlush(payment).paymentId
    }

    override fun findById(paymentId: UUID): Payment? {
        return paymentJpa.findById(paymentId).orElse(null)
    }

    override fun findPaymentIdForIdempotencyKey(
        orderId: UUID,
        idempotencyKey: UUID
    ): UUID? {
        return paymentJpa.findByOrderIdAndIdempotencyKey(orderId, idempotencyKey)?.paymentId
    }

    override fun getProviderPaymentIdByPaymentId(paymentId: UUID): UUID? {
        return paymentJpa.findProviderPaymentIdByPaymentId(paymentId)
    }
}

interface PaymentJpa : JpaRepository<Payment, UUID> {
    fun findByOrderIdAndIdempotencyKey(orderId: UUID, idempotencyKey: UUID): Payment?

    @Query("SELECT p.providerPaymentId FROM Payment p WHERE p.paymentId = :paymentId")
    fun findProviderPaymentIdByPaymentId(paymentId: UUID): UUID?
}