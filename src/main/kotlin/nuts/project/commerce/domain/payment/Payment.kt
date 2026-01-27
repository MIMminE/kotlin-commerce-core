package nuts.project.commerce.domain.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import nuts.project.commerce.domain.common.Money
import java.util.UUID

@Entity
@Table(
    name = "payment",
    indexes = [
        Index(name = "idx_payment_order", columnList = "order_id")
    ]
)
class Payment protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID()

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    lateinit var orderId: UUID
        protected set

    @Column(name = "amout", nullable = false)
    lateinit var amount: Money
        protected set

    @Column(name = "pg_provider", length = 50)
    var pgProvider: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.APPROVED
        protected set

    @Column(name = "pg_payment_key", nullable = false, length = 100)
    lateinit var pgPaymentKey: String
        protected set

    @Column(name = "idempotency_key", nullable = false, length = 80)
    lateinit var idempotencyKey: String
        protected set

//    companion object {
//        fun approved(orderId: UUID, idempotencyKey: String): Payment {
//            require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
//            return Payment().apply {
//                this.orderId = orderId
//                this.status = PaymentStatus.APPROVED
//                this.idempotencyKey = idempotencyKey.trim()
//            }
//        }
//
//        fun declined(orderId: UUID, idempotencyKey: String): Payment {
//            require(idempotencyKey.isNotBlank()) { "idempotencyKey must not be blank" }
//            return Payment().apply {
//                this.orderId = orderId
//                this.status = PaymentStatus.DECLINED
//                this.idempotencyKey = idempotencyKey.trim()
//            }
//        }
//    }
}