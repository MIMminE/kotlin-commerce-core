package nuts.commerce.paymentservice.adapter.repository

import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import java.util.UUID

class InMemoryPaymentRepository : PaymentRepository {
    private val payments = mutableMapOf<UUID, Payment>()
    private val idempotencyIndex = mutableMapOf<IdempotencyKey, UUID>()

    override fun save(payment: Payment): UUID {
        payments[payment.paymentId] = payment
        idempotencyIndex[IdempotencyKey(payment.orderId, payment.idempotencyKey)] = payment.paymentId
        return payment.paymentId
    }

    override fun findById(paymentId: UUID): Payment? {
        return payments[paymentId]
    }

    override fun findPaymentIdForIdempotencyKey(orderId: UUID, idempotencyKey: UUID): UUID? {
        return idempotencyIndex[IdempotencyKey(orderId, idempotencyKey)]
    }

    override fun getProviderPaymentIdByPaymentId(paymentId: UUID): UUID? {
        return payments[paymentId]?.providerPaymentId
    }

    fun clear() {
        payments.clear()
        idempotencyIndex.clear()
    }

    private data class IdempotencyKey(val orderId: UUID, val idempotencyKey: UUID)
}

