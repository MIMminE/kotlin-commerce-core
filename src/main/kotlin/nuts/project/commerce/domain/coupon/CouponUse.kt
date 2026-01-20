package nuts.project.commerce.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "coupon_use",
    indexes = [
        Index(name = "idx_coupon_use_coupon_id", columnList = "order_id")
    ]
)
class CouponUse protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()
        protected set

    @Column(name = "coupon_id", nullable = false, columnDefinition = "uuid")
    lateinit var couponId: UUID
        protected set

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    lateinit var userId: UUID
        protected set

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    lateinit var orderId: UUID
        protected set

    @Column(name = "used_at", nullable = false)
    lateinit var usedAt: Instant
        protected set

    companion object {
        fun create(couponId: UUID, userId: UUID, orderId: UUID, usedAt: Instant): CouponUse =
            CouponUse().apply {
                this.couponId = couponId
                this.userId = userId
                this.orderId = orderId
                this.usedAt = usedAt
            }
    }
}