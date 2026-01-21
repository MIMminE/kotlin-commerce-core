package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.CouponRepositoryPort
import nuts.project.commerce.domain.coupon.Coupon
import nuts.project.commerce.infrastructure.repository.JpaCouponRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CouponRepository(private val jpaCouponRepository: JpaCouponRepository) : CouponRepositoryPort {

    override fun save(coupon: Coupon): Coupon {
        return jpaCouponRepository.save(coupon)
    }

    override fun findById(id: UUID): Coupon? {
        return jpaCouponRepository.findById(id).orElse(null)
    }
}