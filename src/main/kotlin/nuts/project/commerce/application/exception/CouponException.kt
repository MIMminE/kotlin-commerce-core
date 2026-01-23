package nuts.project.commerce.application.exception

import java.util.UUID

open class CouponException(message: String) : RuntimeException(message)

class CouponNotFoundException(couponId: UUID) :
    CouponException("쿠폰을 찾을 수 없습니다. couponId=$couponId")

class InvalidCouponException(couponId: UUID) :
    CouponException("유효하지 않은 쿠폰입니다. couponId=$couponId")
