package nuts.project.commerce.application.port.repository

import nuts.project.commerce.domain.coupon.Coupon
import java.util.UUID

interface CouponRepositoryPort {
    fun save(coupon: Coupon) : Coupon
    fun findById(id: UUID) : Coupon?
}