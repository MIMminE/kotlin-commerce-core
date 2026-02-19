package nuts.commerce.orderservice.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_sagas")
class OrderSaga protected constructor(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val sageId: UUID,

    @Column(name = "order_id", nullable = false, updatable = false)
    val orderId: UUID,

    @Column(name = "total_price", nullable = false, updatable = false)
    val totalPrice: Long,

    @Column(name = "currency", nullable = false, updatable = false, length = 8)
    val currency: String,

    @Column(name = "reservation_id")
    var reservationId: UUID? = null,

    @Column(name = "payment_id")
    var paymentId: UUID? = null,

    @Column(name = "reservation_requested_at")
    var reservationRequestedAt: Instant,

    @Column(name = "reservation_reserved_at")
    var reservationReservedAt: Instant?,

    @Column(name = "reservation_released_at")
    var reservationReleasedAt: Instant?,

    @Column(name = "payment_requested_at")
    var paymentRequestedAt: Instant?,

    @Column(name = "payment_completed_at")
    var paymentCompletedAt: Instant?,

    @Column(name = "payment_released_at")
    var paymentReleasedAt: Instant?,

    @Column(name = "completed_at")
    var completedAt: Instant?,

    @Column(name = "failed_at")
    var failedAt: Instant?,

    @Column(name = "fail_reason")
    val failReason: String? = null,

    @Version
    var version: Long? = null
) : BaseEntity() {

    companion object {

        fun create(
            sageId: UUID = UUID.randomUUID(),
            orderId: UUID,
            totalPrice: Long,
            currency: String,
            reservationRequestedAt: Instant,
            reservationReservedAt: Instant? = null,
            reservationReleasedAt: Instant? = null,
            paymentRequestedAt: Instant? = null,
            paymentCompletedAt: Instant? = null,
            paymentReleasedAt: Instant? = null,
            completedAt: Instant? = null,
            failedAt: Instant? = null,
        ): OrderSaga {
            return OrderSaga(
                sageId = sageId,
                orderId = orderId,
                totalPrice = totalPrice,
                currency = currency,
                reservationRequestedAt = reservationRequestedAt,
                reservationReservedAt = reservationReservedAt,
                reservationReleasedAt = reservationReleasedAt,
                paymentRequestedAt = paymentRequestedAt,
                paymentCompletedAt = paymentCompletedAt,
                paymentReleasedAt = paymentReleasedAt,
                completedAt = completedAt,
                failedAt = failedAt
            )
        }
    }
}