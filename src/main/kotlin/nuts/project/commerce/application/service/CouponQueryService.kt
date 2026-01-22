package nuts.project.commerce.application.service

import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.domain.coupon.Coupon
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CouponQueryService(private val couponRepository: CouponRepository) {

    fun findCoupon(couponId: UUID) : Coupon {
        return couponRepository.findById(couponId)?: throw NoSuchElementException("Coupon not found with id: $couponId")
    }
}