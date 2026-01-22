package nuts.project.commerce.application.port.command

import nuts.project.commerce.domain.coupon.Coupon

interface CouponCommand {
    fun save(coupon: Coupon) : Coupon
}