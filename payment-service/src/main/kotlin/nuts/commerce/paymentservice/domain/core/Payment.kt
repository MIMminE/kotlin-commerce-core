package nuts.commerce.paymentservice.domain.core

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
import nuts.commerce.paymentservice.domain.BaseEntity
import nuts.commerce.paymentservice.domain.Money
import nuts.commerce.paymentservice.domain.PaymentStatus
import java.util.UUID

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "ix_payments_order_id", columnList = "order_id"),
        Index(name = "ix_payments_user_id", columnList = "user_id"),
        Index(name = "ix_payments_status", columnList = "status")
    ]
)
class Payment protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    lateinit var id: UUID
        protected set

    @Column(name = "order_id", nullable = false, updatable = false)
    lateinit var orderId: UUID
        protected set

    @Column(name = "user_id", nullable = false, length = 64, updatable = false)
    lateinit var userId: String
        protected set

    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "amount", nullable = false)),
        AttributeOverride(name = "currency", column = Column(name = "currency", nullable = false, length = 8))
    )
    lateinit var amount: Money
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    var status: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    @Column(name = "payment_key", length = 128)
    var paymentKey: String? = null
        protected set

    @Column(name = "failure_reason", length = 255)
    var failureReason: String? = null
        protected set

    companion object {
        fun request(
            orderId: UUID,
            userId: String,
            amount: Money,
            idGenerator: () -> UUID = { UUID.randomUUID() }
        ): Payment {
            require(userId.isNotBlank()) { "userId is required" }
            require(amount.amount > 0L) { "amount must be > 0" }

            return Payment().apply {
                this.id = idGenerator()
                this.orderId = orderId
                this.userId = userId
                this.amount = amount
                this.status = PaymentStatus.REQUESTED
            }
        }
    }

    fun approve(paymentKey: String) {
        require(status == PaymentStatus.REQUESTED) { "invalid transition: $status -> APPROVED" }
        require(paymentKey.isNotBlank()) { "paymentKey is required" }

        this.status = PaymentStatus.APPROVED
        this.paymentKey = paymentKey
        this.failureReason = null
    }

    fun fail(reason: String) {
        require(status == PaymentStatus.REQUESTED) { "invalid transition: $status -> FAILED" }
        require(reason.isNotBlank()) { "reason is required" }

        this.status = PaymentStatus.FAILED
        this.failureReason = reason
    }
}
