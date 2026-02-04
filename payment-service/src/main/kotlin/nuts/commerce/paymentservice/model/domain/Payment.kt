package nuts.commerce.paymentservice.model.domain

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
import nuts.commerce.paymentservice.model.BaseEntity
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
    private val paymentId: UUID,

    @Column(nullable = false, updatable = false)
    private val orderId: UUID,

    @Embedded
    @Column(nullable = false)
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "unit_price_amount", nullable = false)),
        AttributeOverride(
            name = "currency",
            column = Column(name = "unit_price_currency", nullable = false, length = 8)
        )
    )
    private val money: Money,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private var status: PaymentStatus,

    private var paymentMethodType: PaymentMethodType?,

    @Column(nullable = false, updatable = false)
    private val idempotencyKey: UUID,

    ) : BaseEntity() {

    // Public accessors to avoid reflection by callers
    fun paymentId(): UUID = paymentId
    fun orderId(): UUID = orderId
    fun status(): PaymentStatus = status
    fun idempotencyKey(): UUID = idempotencyKey

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
                idempotencyKey = idempotencyKey
            )
        }
    }

    fun startProcessing(now: Instant) {
        require(status == PaymentStatus.CREATED) { "invalid transition $status -> PROCESSING" }
        status = PaymentStatus.PROCESSING
        updatedAt = now
    }

    fun approve(now: Instant, providerPaymentId: String?) {
        require(status == PaymentStatus.PROCESSING) { "invalid transition $status -> APPROVED" }
        status = PaymentStatus.APPROVED
        updatedAt = now
    }

    fun decline(now: Instant, reason: String?) {
        require(status == PaymentStatus.PROCESSING) { "invalid transition $status -> DECLINED" }
        status = PaymentStatus.DECLINED
        updatedAt = now
    }
}

enum class PaymentStatus { CREATED, PROCESSING, APPROVED, DECLINED, FAILED }

enum class PaymentMethodType { CREDIT_CARD, PAYPAL, BANK_TRANSFER }
