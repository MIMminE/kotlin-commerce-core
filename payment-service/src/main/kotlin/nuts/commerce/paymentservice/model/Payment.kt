package nuts.commerce.paymentservice.model

import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import nuts.commerce.paymentservice.exception.PaymentException
import java.util.UUID

@Entity
@Table(
    name = "payments",
    uniqueConstraints = [
        UniqueConstraint(name = "uc_payments_idempotency_key", columnNames = ["orderId", "idempotencyKey"])
    ]
)
class Payment protected constructor(

    @Id
    val paymentId: UUID,

    @Column(nullable = false, updatable = false)
    val orderId: UUID,

    @Column(nullable = false, updatable = false)
    val idempotencyKey: UUID,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus,

    @Embedded
    @Column(nullable = false, updatable = false)
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "unit_price_amount", nullable = false)),
        AttributeOverride(
            name = "currency",
            column = Column(name = "unit_price_currency", nullable = false, length = 8)
        )
    )
    val money: Money,

    var paymentProvider: String?,
    var providerPaymentId: UUID?,

    @Version
    var version: Long? = null

) : BaseEntity() {

    companion object {
        fun create(
            paymentId: UUID = UUID.randomUUID(),
            orderId: UUID,
            idempotencyKey: UUID,
            status: PaymentStatus = PaymentStatus.CREATED,
            money: Money,
            paymentProvider: String? = null,
            providerPaymentId: UUID? = null,
        ): Payment {
            return Payment(
                paymentId = paymentId,
                orderId = orderId,
                money = money,
                status = status,
                idempotencyKey = idempotencyKey,
                paymentProvider = paymentProvider,
                providerPaymentId = providerPaymentId
            )
        }
    }

    fun approve(paymentProvider: String, providerPaymentId: UUID) {
        if (this.status != PaymentStatus.CREATED) {
            throw PaymentException.InvalidCommand("Only payments in CREATED status can be approved. Current status: ${this.status}")
        }
        this.status = PaymentStatus.APPROVED
        this.paymentProvider = paymentProvider
        this.providerPaymentId = providerPaymentId
    }

    fun fail(reason: String) {
        if (this.status != PaymentStatus.CREATED) {
            throw PaymentException.InvalidCommand("Only payments in CREATED status can be failed. Current status: ${this.status} reason: $reason")
        }
        this.status = PaymentStatus.FAILED
    }

    fun commit() {
        if (this.status != PaymentStatus.APPROVED) {
            throw PaymentException.InvalidCommand("Only payments in APPROVED status can be committed. Current status: ${this.status}")
        }
        this.status = PaymentStatus.COMMITED
    }

    fun release() {
        if (this.status != PaymentStatus.APPROVED) {
            throw PaymentException.InvalidCommand("Only payments in APPROVED status can be canceled. Current status: ${this.status}")
        }
        this.status = PaymentStatus.RELEASED
    }
}

enum class PaymentStatus { CREATED, APPROVED, COMMITED, FAILED, RELEASED }