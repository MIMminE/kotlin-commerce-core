package nuts.project.commerce.application.service

import nuts.project.commerce.application.exception.CouponNotFoundException
import nuts.project.commerce.application.exception.InvalidCouponException
import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.domain.coupon.Coupon
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class CouponService(private val couponRepository: CouponRepository) {
    fun save(coupon: Coupon): Coupon {
        return couponRepository.save(coupon)
    }
      fun getValidCoupon(couponId: UUID, orderAmount: Long): Coupon {

        val coupon = couponRepository.findById(couponId)
            ?: throw CouponNotFoundException(couponId)

        val now = Instant.now()

        if (!coupon.isActiveAt(now)) throw InvalidCouponException(couponId)
        if (!coupon.canApplyTo(orderAmount)) throw InvalidCouponException(couponId)

        return coupon
    }
}