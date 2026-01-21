package nuts.project.commerce.application.port.coupon

import java.util.UUID

interface CouponPolicyPort {
    fun calculateDiscount(userId: UUID, couponId: UUID, originalAmount: Long): CouponDiscountResult
}

data class CouponDiscountResult(val couponId: UUID, val discountAmount: Long)