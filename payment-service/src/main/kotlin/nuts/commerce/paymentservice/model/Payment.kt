package nuts.commerce.paymentservice.model

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import nuts.commerce.paymentservice.exception.PaymentException
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_payments_order_id", columnList = "orderId")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uc_payments_idempotency_key", columnNames = ["idempotencyKey"])
    ]
)
class Payment protected constructor(
    @Id
    val paymentId: UUID,

    @Column(nullable = false, updatable = false)
    val orderId: UUID,

    @Embedded
    @Column(nullable = false)
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "unit_price_amount", nullable = false)),
        AttributeOverride(
            name = "currency",
            column = Column(name = "unit_price_currency", nullable = false, length = 8)
        )
    )
    var money: Money,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus,

    @Column(name = "payment_method_type")
    @Enumerated(EnumType.STRING)
    var paymentMethodType: PaymentMethodType?,

    @Column(nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            paymentId: UUID = UUID.randomUUID(),
            orderId: UUID,
            money: Money,
            status: PaymentStatus = PaymentStatus.CREATED,
            paymentMethodType: PaymentMethodType? = null,
            idempotencyKey: UUID,
        ): Payment {
            return Payment(
                paymentId = paymentId,
                orderId = orderId,
                money = money,
                status = status,
                paymentMethodType = paymentMethodType,
                idempotencyKey = idempotencyKey,
                version = null
            )
        }
    }

    fun startProcessing(now: Instant) {
        if (status != PaymentStatus.CREATED) {
            throw PaymentException.InvalidTransition(
                paymentId = paymentId,
                from = status,
                to = PaymentStatus.PROCESSING
            )
        }
        status = PaymentStatus.PROCESSING
        updatedAt = now
    }

    fun approve(now: Instant) {
        if (status != PaymentStatus.PROCESSING) {
            throw PaymentException.InvalidTransition(paymentId = paymentId, from = status, to = PaymentStatus.APPROVED)
        }
        status = PaymentStatus.APPROVED
        updatedAt = now
    }

    fun decline(now: Instant) {
        if (status != PaymentStatus.PROCESSING) {
            throw PaymentException.InvalidTransition(paymentId = paymentId, from = status, to = PaymentStatus.DECLINED)
        }
        status = PaymentStatus.DECLINED
        updatedAt = now
    }

    fun fail(now: Instant) {
        if (status != PaymentStatus.PROCESSING && status != PaymentStatus.CREATED) {
            throw PaymentException.InvalidTransition(paymentId = paymentId, from = status, to = PaymentStatus.FAILED)
        }
        status = PaymentStatus.FAILED
        updatedAt = now
    }

    fun updatePaymentMethodType(type: PaymentMethodType?) {
        this.paymentMethodType = type
    }
}

enum class PaymentStatus { CREATED, PROCESSING, APPROVED, DECLINED, FAILED }

enum class PaymentMethodType { CREDIT_CARD, PAYPAL, BANK_TRANSFER }
