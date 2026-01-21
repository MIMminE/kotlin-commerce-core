package nuts.project.commerce.infrastructure.repository

import nuts.project.commerce.domain.coupon.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaCouponRepository : JpaRepository<Coupon, UUID> {
}