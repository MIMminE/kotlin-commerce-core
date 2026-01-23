package nuts.project.commerce.infrastructure.adapter

import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.domain.coupon.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JpaCouponRepository(private val couponJpa: CouponJpa) : CouponRepository {
    override fun findById(couponId: UUID): Coupon? {
        TODO("Not yet implemented")
    }

    override fun save(coupon: Coupon): Coupon {
        TODO("Not yet implemented")
    }

    interface CouponJpa : JpaRepository<Coupon, UUID>
}

