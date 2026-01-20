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
    }

    fun canApply(now: Instant, originalAmount: Long): Boolean {
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


    enum class CouponType {
        FIXED_AMOUNT,
        PERCENT
    }
}