package nuts.project.commerce.application.port.query

import nuts.project.commerce.domain.coupon.Coupon
import java.util.UUID

interface CouponQuery {
    fun findById(id: UUID) : Coupon?
}