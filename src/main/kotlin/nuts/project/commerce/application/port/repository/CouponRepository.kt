package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.coupon.Coupon
import java.util.UUID

interface CouponRepository {
    fun findById(couponId: UUID) : Coupon?
    fun save(coupon: Coupon) : Coupon
}