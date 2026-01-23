package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.domain.coupon.Coupon
import org.springframework.stereotype.Service

@Service
class CouponCommandService(private val couponRepository: CouponRepository) {
    fun save(coupon: Coupon): Coupon {
        return couponRepository.save(coupon)
    }
}