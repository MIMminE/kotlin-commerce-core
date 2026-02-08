package nuts.commerce.paymentservice.application.port.repository

import nuts.commerce.paymentservice.model.Payment
import nuts.commerce.paymentservice.port.repository.PaymentRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPaymentRepository : PaymentRepository {

    private val byId = ConcurrentHashMap<UUID, Payment>()
    private val byOrderId = ConcurrentHashMap<UUID, UUID>()

    fun clear() {
        byId.clear()
        byOrderId.clear()
    }

    override fun save(payment: Payment): Payment {
        val id = payment.paymentId
        byId[id] = payment
        byOrderId[payment.orderId] = id
        return payment
    }

    override fun findById(paymentId: UUID): Payment? = byId[paymentId]

    override fun findByOrderId(orderId: UUID): Payment? {
        val id = byOrderId[orderId] ?: return null
        return byId[id]
    }
}