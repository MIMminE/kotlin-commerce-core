package nuts.commerce.orderservice.adapter.repository

import nuts.commerce.orderservice.port.repository.SageRepository
import nuts.commerce.orderservice.model.OrderSaga
import nuts.commerce.orderservice.port.repository.OrderSagaInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaSageRepository(private val sagaJpa: OrderSagaJpa) : SageRepository {
    override fun save(saga: OrderSaga): OrderSaga = sagaJpa.saveAndFlush(saga)

    override fun findByOrderId(orderId: UUID): OrderSaga? = sagaJpa.findByOrderId(orderId)
    override fun findSageInfoByOrderId(orderId: UUID): OrderSagaInfo? {
        return sagaJpa.findSagaInfoByOrderId(orderId)
    }

    override fun setReservationId(orderId: UUID, reservationId: UUID) {
        val n = sagaJpa.setReservationId(orderId, reservationId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun setPaymentId(orderId: UUID, paymentId: UUID) {
        val n = sagaJpa.setPaymentId(orderId, paymentId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markReservationCompleteAt(orderId: UUID) {
        val n = sagaJpa.markReservationCompleteAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markReservationReleaseAt(orderId: UUID, reason: String) {
        val n = sagaJpa.markReservationReleaseAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
        val failUpdateCount = sagaJpa.markFailedAt(orderId, reason)
        if (failUpdateCount == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markPaymentRequestAt(orderId: UUID) {
        val n = sagaJpa.markPaymentRequestAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markPaymentCompleteAt(orderId: UUID) {
        val n = sagaJpa.markPaymentCompleteAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markPaymentReleaseAt(orderId: UUID) {
        val n = sagaJpa.markPaymentReleaseAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markFailedAt(orderId: UUID, reason: String) {
        val n = sagaJpa.markFailedAt(orderId, reason)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }

    override fun markCompleteAt(orderId: UUID) {
        val n = sagaJpa.markCompleteAt(orderId)
        if (n == 0) {
            throw IllegalStateException("OrderSaga not found for orderId: $orderId")
        }
    }
}

interface OrderSagaJpa : JpaRepository<OrderSaga, UUID> {
    fun findByOrderId(orderId: UUID): OrderSaga?

    @Query(
        """
        SELECT new nuts.commerce.orderservice.port.repository.OrderSagaInfo(
            s.orderId, s.reservationId, s.paymentId, s.totalPrice, s.currency
        )
        FROM OrderSaga s
        WHERE s.orderId = :orderId
        """
    )
    fun findSagaInfoByOrderId(orderId: UUID): OrderSagaInfo?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.reservationReservedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markReservationCompleteAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.reservationReleasedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markReservationReleaseAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.paymentRequestedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markPaymentRequestAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.paymentCompletedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markPaymentCompleteAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.paymentReleasedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markPaymentReleaseAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.failedAt = CURRENT_TIMESTAMP, s.failReason = :reason
        WHERE s.orderId = :orderId
        """
    )
    fun markFailedAt(orderId: UUID, reason: String): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.completedAt = CURRENT_TIMESTAMP
        WHERE s.orderId = :orderId
        """
    )
    fun markCompleteAt(orderId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.reservationId = :reservationId
        WHERE s.orderId = :orderId
        """
    )
    fun setReservationId(orderId: UUID, reservationId: UUID): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE OrderSaga s
           SET s.paymentId = :paymentId
        WHERE s.orderId = :orderId
        """
    )
    fun setPaymentId(orderId: UUID, paymentId: UUID): Int

}
