package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.coupon.CouponDiscountResult
import nuts.project.commerce.application.port.coupon.CouponPolicyPort
import nuts.project.commerce.application.port.repository.CouponRepositoryPort
import org.springframework.stereotype.Service
import java.util.*

@Service
class CouponPolicyAdapter(
    private val couponRepository: CouponRepositoryPort
) : CouponPolicyPort {

    override fun calculateDiscount(userId: UUID, couponId: UUID, originalAmount: Long): CouponDiscountResult {
        val coupon =
            couponRepository.findById(couponId) ?: throw IllegalArgumentException("Coupon not found for id=$couponId")

        val discount = coupon.calculateDiscount(originalAmount)
        return CouponDiscountResult(couponId = couponId, discountAmount = discount)
    }
}