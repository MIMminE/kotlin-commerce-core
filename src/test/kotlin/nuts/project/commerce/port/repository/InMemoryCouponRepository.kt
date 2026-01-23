package nuts.project.commerce.port.repository

import nuts.project.commerce.application.port.repository.CouponRepository
import nuts.project.commerce.domain.coupon.Coupon
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryCouponRepository : CouponRepository {

    private val store = ConcurrentHashMap<UUID, Coupon>()

    override fun findById(couponId: UUID): Coupon? =
        store[couponId]

    override fun save(coupon: Coupon): Coupon {
        store[coupon.id] = coupon
        return coupon
    }

    fun clear() = store.clear()
}