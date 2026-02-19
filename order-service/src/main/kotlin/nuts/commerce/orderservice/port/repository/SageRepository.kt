package nuts.commerce.orderservice.port.repository

import nuts.commerce.orderservice.model.OrderSaga
import java.util.UUID

interface SageRepository {
    fun save(saga: OrderSaga): OrderSaga
    fun findByOrderId(orderId: UUID): OrderSaga?
    fun findSageInfoByOrderId(orderId: UUID): OrderSagaInfo?

    fun setReservationId(orderId: UUID, reservationId: UUID)
    fun setPaymentId(orderId: UUID, paymentId: UUID)

    fun markReservationCompleteAt(orderId: UUID)
    fun markReservationReleaseAt(orderId: UUID, reason: String)

    fun markPaymentRequestAt(orderId: UUID)
    fun markPaymentCompleteAt(orderId: UUID)
    fun markPaymentReleaseAt(orderId: UUID)

    fun markFailedAt(orderId: UUID, reason: String)
    fun markCompleteAt(orderId: UUID)
}

data class OrderSagaInfo(
    val orderId: UUID,
    val reservationId: UUID?,
    val paymentId: UUID?,
    val totalPrice: Long?,
    val currency: String?
)