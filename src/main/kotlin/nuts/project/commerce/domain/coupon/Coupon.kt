package nuts.project.commerce.domain.coupon

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import nuts.project.commerce.domain.common.BaseEntity
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "coupon")
class Coupon protected constructor() : BaseEntity() {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID()
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    lateinit var type: CouponType
        protected set

    @Column(name = "value", nullable = false)
    var value: Long = 0
        protected set

    @Column(name = "min_order_amount", nullable = false)
    var minOrderAmount: Long = 0
        protected set


    @Column(name = "valid_from", nullable = false)
    lateinit var validFrom: Instant
        protected set

    @Column(name = "valid_to", nullable = false)
    lateinit var validTo: Instant
        protected set

    @Column(name = "active", nullable = false)
    var active: Boolean = true
        protected set

    fun canApply(originalAmount: Long): Boolean {
        val now = Instant.now()
        if (!active) return false
        if (now.isBefore(validFrom) || now.isAfter(validTo)) return false
        if (originalAmount < minOrderAmount) return false
        return true
    }

    fun calculateDiscount(originalAmount: Long): Long {
        require(originalAmount >= 0) { "Original amount must be non-negative" }
        return when (type) {
            CouponType.FIXED_AMOUNT -> minOf(value, originalAmount)
            CouponType.PERCENT -> (originalAmount * value) / 100
        }
    }

    companion object {
        fun create(
            type: CouponType,
            value: Long,
            minOrderAmount: Long,
            validFrom: Instant,
            validTo: Instant,
            active: Boolean = true
        ): Coupon {
            require(value > 0) { "Coupon value must be positive" }
            require(minOrderAmount >= 0) { "Minimum order amount must be non-negative" }
            require(validFrom.isBefore(validTo)) { "Valid from must be before valid to" }

            return Coupon().apply {
                this.type = type
                this.value = value
                this.minOrderAmount = minOrderAmount
                this.validFrom = validFrom
                this.validTo = validTo
                this.active = active
            }
        }

        // 기간에 제한이 없는 쿠폰 생성 편의 메서드
        fun create(
            type: CouponType,
            value: Long,
            minOrderAmount: Long
        ): Coupon {
            val now = Instant.now()
            val farFuture = now.plusSeconds(60L * 60 * 24 * 365 * 100) // 100년 후
            return create(
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                validFrom = now,
                validTo = farFuture,
                active = true
            )
        }
    }


}