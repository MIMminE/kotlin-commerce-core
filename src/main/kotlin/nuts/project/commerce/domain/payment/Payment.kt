package nuts.project.commerce.domain.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import nuts.project.commerce.domain.common.BaseEntity
import nuts.project.commerce.domain.common.Money
import java.util.UUID

@Entity
@Table(
    name = "payment",
    indexes = [
        Index(name = "idx_payment_order", columnList = "order_id"),
        Index(name = "ux_payment_idempotency_key", columnList = "idempotency_key", unique = true)
    ]
)
class Payment protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    lateinit var orderId: UUID
        protected set

    @Column(name = "amount", nullable = false)
    lateinit var amount: Money
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.APPROVED
        protected set

    @Column(name = "pg_provider", length = 50)
    var pgProvider: String? = null

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    lateinit var idempotencyKey: String
        protected set


    @Version
    @Column(nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun create(orderId: UUID, amount: Money, idempotencyKey: String): Payment {
            return Payment().apply {
                this.orderId = orderId
                this.amount = amount
                this.status = PaymentStatus.INITIATED
                this.idempotencyKey = idempotencyKey
            }
        }
    }
}