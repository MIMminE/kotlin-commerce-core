package nuts.commerce.orderservice.testutil

import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.port.repository.OrderSagaInfo
import nuts.commerce.orderservice.port.repository.SageRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemorySageRepository(
    private val nowProvider: () -> Instant = { Instant.now() }
) : SageRepository {

    private val store: MutableMap<UUID, OrderSaga> = ConcurrentHashMap()

    fun clear() = store.clear()

    override fun save(saga: OrderSaga): OrderSaga {
        store[saga.orderId] = saga
        return saga
    }

    override fun findByOrderId(orderId: UUID): OrderSaga? {
        return store[orderId]
    }

    override fun findSageInfoByOrderId(orderId: UUID): OrderSagaInfo? {
        val saga = store[orderId] ?: return null
        return OrderSagaInfo(
            orderId = saga.orderId,
            reservationId = saga.reservationId,
            paymentId = saga.paymentId,
            totalPrice = saga.totalPrice,
            currency = saga.currency
        )
    }

    override fun setReservationId(orderId: UUID, reservationId: UUID) {
        val saga = store[orderId] ?: return
        saga.reservationId = reservationId
    }

    override fun setPaymentId(orderId: UUID, paymentId: UUID) {
        val saga = store[orderId] ?: return
        saga.paymentId = paymentId
    }

    override fun markReservationCompleteAt(orderId: UUID) {
        val saga = store[orderId] ?: return
        saga.reservationReservedAt = nowProvider()
    }

    override fun markReservationReleaseAt(orderId: UUID, reason: String) {
        val saga = store[orderId] ?: return
        saga.reservationReleasedAt = nowProvider()
    }

    override fun markPaymentRequestAt(orderId: UUID) {
        val saga = store[orderId] ?: return
        saga.paymentRequestedAt = nowProvider()
    }

    override fun markPaymentCompleteAt(orderId: UUID) {
        val saga = store[orderId] ?: return
        saga.paymentCompletedAt = nowProvider()
    }

    override fun markPaymentReleaseAt(orderId: UUID) {
        val saga = store[orderId] ?: return
        saga.paymentReleasedAt = nowProvider()
    }

    override fun markFailedAt(orderId: UUID, reason: String) {
        val saga = store[orderId] ?: return
        saga.failedAt = nowProvider()
    }

    override fun markCompleteAt(orderId: UUID) {
        val saga = store[orderId] ?: return
        saga.completedAt = nowProvider()
    }

    fun findAll(): List<OrderSaga> {
        return store.values.toList()
    }
}

